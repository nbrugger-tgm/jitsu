package eu.nitok.jitsu.compiler.ast;

import com.niton.parser.ast.LocatableReducedNode
import eu.nitok.jitsu.compiler.ast.ExpressionNode.*
import eu.nitok.jitsu.compiler.ast.ExpressionNode.NumberLiteralNode.IntegerLiteralNode
import eu.nitok.jitsu.compiler.ast.StatementNode.*
import eu.nitok.jitsu.compiler.ast.StatementNode.AssignmentNode.AssignmentTarget
import eu.nitok.jitsu.compiler.ast.StatementNode.CodeBlockNode.SingleExpressionCodeBlock
import eu.nitok.jitsu.compiler.ast.StatementNode.CodeBlockNode.StatementsCodeBlock
import eu.nitok.jitsu.compiler.ast.StatementNode.FunctionDeclarationNode.ParameterNode
import eu.nitok.jitsu.compiler.ast.StatementNode.IfNode.ElseNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseBodyNode.CodeBlockCaseBodyNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseBodyNode.ExpressionCaseBodyNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode.ConditionCaseNode.CaseMatchingNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode.ConditionCaseNode.CaseMatchingNode.DeconstructPatternMatch.Variable
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode.DefaultCaseNode
import eu.nitok.jitsu.compiler.ast.TypeNode.*
import eu.nitok.jitsu.compiler.parser.AssignmentTargetType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.function.UnaryOperator
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.typeOf

typealias RawNode = LocatableReducedNode;
typealias Location = @Serializable(with = LocationSerializer::class) com.niton.parser.token.Location

fun buildFileAst(node: RawNode): @Contextual List<StatementNode> {
    return node.children.map { buildStatement(it) }
}

fun buildStatement(statement: RawNode): StatementNode {
    val (type, value) = nodeType<StatementType>(statement) { it.replace("_WITH_SEMICOLON", "") }
    return when (type) {
        StatementType.VARIABLE_DECLARATION -> buildVariableDeclaration(value)
        StatementType.FUNCTION_DECLARATION -> buildFunction(value)
        StatementType.FUNCTION_CALL -> buildFunctionCall(value)
        StatementType.METHOD_INVOCATION -> buildMethodInvocation(value)
        StatementType.SEMICOLON_STATEMENT -> buildStatement(value)
        StatementType.RETURN_STATEMENT -> buildReturnStatement(value)
        StatementType.ASSIGNMENT -> buildAssignment(value)
        StatementType.IF_STATEMENT -> buildIfStatement(value)
        StatementType.CODE_BLOCK -> buildCodeBlock(value)
        StatementType.SWITCH_STATEMENT -> buildSwitch(value)
        StatementType.YIELD_STATEMENT -> buildYieldStatement(value)
        StatementType.TYPE_DEFINITION -> buildTypeDef(value)
    };
}

fun buildTypeDef(value: RawNode): StatementNode {
    val nameNode = value.getSubNode("name").orElseThrow()
    val name = nameNode.value;
    val type = buildType(value.getSubNode("type").orElseThrow());
    return TypeDefinitionNode(name, type, value.location, nameNode.location, keywordPos(value));
}

fun keywordPos(value: RawNode): Location {
    return value.getSubNode("keyword").orElseThrow().location;
}

fun buildSwitch(value: RawNode): StatementNode {
    return SwitchNode(
        buildExpression(value.getSubNode("item").orElseThrow()),
        value.getSubNode("cases").orElseThrow().children.map {
            buildCase(it)
        },
        value.location,
        keywordPos(value)
    );
}

fun buildCase(node: LocatableReducedNode) : SwitchNode.CaseNode {
    val (matchingType, matching) = nodeType<CaseMatchType>(node.getSubNode("matching").orElseThrow());
    val (bodyType, body) = nodeType<CaseBodyType>(node.getSubNode("body").orElseThrow());
    return SwitchNode.CaseNode(
        when(matchingType) {
            CaseMatchType.CONSTANT_CASE -> buildConstantCase(matching)
            CaseMatchType.CONDITION_CASE -> buildConditionCase(matching)
            CaseMatchType.DEFAULT_CASE -> DefaultCaseNode(matching.location)
        },
        when(bodyType) {
            CaseBodyType.CODE_BLOCK_CASE_BODY -> CodeBlockCaseBodyNode(
                buildCodeBlock(body.getSubNode("body").orElseThrow()),
                body.location
            )
            CaseBodyType.EXPRESSION_CASE_BODY -> ExpressionCaseBodyNode(
                buildExpression(body.getSubNode("body").orElseThrow()),
                body.location
            )
        },
        keywordPos(node)
    )
}

fun buildConditionCase(matching: RawNode): SwitchNode.CaseNode.CaseMatchNode {
    return SwitchNode.CaseNode.CaseMatchNode.ConditionCaseNode(
        buildType(matching.getSubNode("type").orElseThrow()),
        buildMatching(matching.getSubNode("matching").orElseThrow()),
        matching.location
    )
}

fun buildMatching(orElseThrow: LocatableReducedNode): CaseMatchingNode {
    val (type, value) = nodeType<CaseMatchingType>(orElseThrow);
    return when(type) {
        CaseMatchingType.DECONSTRUCT_PATTERN_MATCH -> CaseMatchingNode.DeconstructPatternMatch(
            value.getSubNode("variables").orElseThrow().children.map { Variable(it.value, it.location)  },
            value.location
        )
        CaseMatchingType.CASTING_PATTERN_MATCH -> CaseMatchingNode.CastingPatternMatch(
            value.value,
            value.location
        )
    }
}

fun buildConstantCase(value: RawNode): SwitchNode.CaseNode.CaseMatchNode.ConstantCaseNode {
    return SwitchNode.CaseNode.CaseMatchNode.ConstantCaseNode(
        buildLiteral(value),
        value.location
    )
}

fun buildYieldStatement(value: RawNode): StatementNode {
    TODO("not known if needed");
//return YieldNode(buildExpression(value), value.location);
}

fun buildIfStatement(node: RawNode): IfNode {
    val condition = buildExpression(node.getSubNode("condition").orElseThrow());
    val thenCodeBlockNode = buildCodeBlock(node.getSubNode("code").orElseThrow());
    val elseStatement = node.getSubNode("else").map {
        val (type, value) = nodeType<StatementType>(it.getSubNode("code").orElseThrow());
        when (type) {
            StatementType.CODE_BLOCK -> ElseNode.ElseBlockNode(buildCodeBlock(value))
            StatementType.IF_STATEMENT -> ElseNode.ElseIfNode(buildIfStatement(value))
            else -> throw IllegalStateException("Unknown else type: $type");
        }
    }.getOrNull();
    return IfNode(condition, thenCodeBlockNode, elseStatement, node.location, keywordPos(node));
}

fun buildAssignment(value: RawNode): AssignmentNode {
    val targetNode = value.getSubNode("variable").orElseThrow()
    val target = run {
        val (type, target) = nodeType<AssignmentTargetType>(targetNode);
        return@run when (type) {
            AssignmentTargetType.VARIABLE_ASSIGNMENT -> AssignmentTarget.VariableAssignment(target.value, targetNode.location)
            AssignmentTargetType.PROPERTY_ASSIGNMENT -> AssignmentTarget.PropertyAssignment(buildFieldAccess(target))
        }
    };
    val expression = buildExpression(
        value.getSubNode("assignment").orElseThrow()
            .getSubNode("expression").orElseThrow()
    );
    return AssignmentNode(target, expression, value.location, targetNode.location);
}

fun buildReturnStatement(value: RawNode): ReturnNode {
    val expression = value.getSubNode("value").map { buildExpression(it) }.getOrNull();
    return ReturnNode(expression, value.location, keywordPos(value));
}

fun buildMethodInvocation(node: RawNode): MethodInvocationNode {
    var target = buildExpression(node.getSubNode("target").orElseThrow());
    val methodNode = node.getSubNode("method").orElseThrow()
    val parameters = node.getSubNode("parameters").map {
        it.children.map { child -> buildExpression(child) }
    }.orElse(emptyList());
    return MethodInvocationNode(target,methodNode.value, parameters, node.location, methodNode.location);
}

fun buildFunctionCall(node: RawNode): FunctionCallNode {
    val functionNameNode = node.getSubNode("function").orElseThrow()
    val name = functionNameNode.value;
    val parameters = node.getSubNode("parameters").map {
        it.children.map { child -> buildExpression(child) }
    }.orElse(emptyList());
    return FunctionCallNode(name, parameters, node.location, functionNameNode.location);
}

fun buildFunction(node: RawNode): FunctionDeclarationNode {
    val name: String? = node.getSubNode("name").map { e -> e.value }.getOrNull();
    val parameters = node.getSubNode("parameters").map {
        it.children.map { child -> buildParameter(child) }
    }.orElse(emptyList());
    val returnType = node.getSubNode("return_type").map {
        buildType(it.getSubNode("type").orElseThrow())
    }.getOrNull();
    val body = buildCodeBlock(node.getSubNode("body").orElseThrow());
    return FunctionDeclarationNode(name, parameters, returnType, body, node.location,keywordPos(node));
}

fun buildCodeBlock(node: LocatableReducedNode): CodeBlockNode {
    val (type, value) = nodeType<CodeBlockContentType>(node.getSubNode("code").orElseThrow()) {
        CodeBlockContentType.byGrammarName(it).toString()
    };
    return when (type) {
        CodeBlockContentType.EXPRESSION -> SingleExpressionCodeBlock(buildExpression(value), node.location)
        CodeBlockContentType.STATEMENTS -> StatementsCodeBlock(value.children.map { buildStatement(it) }, node.location)
    }
}

fun buildParameter(it: LocatableReducedNode): ParameterNode {
    val nameNode = it.getSubNode("name").orElseThrow()
    val name = nameNode.value;
    val type = buildType(it.getSubNode("type_def").orElseThrow().getSubNode("type").orElseThrow());
    val defaultValue = it.getSubNode("default_value").map {
        buildExpression(it.getSubNode("expression").orElseThrow())
    }.getOrNull();
    return ParameterNode(name, type, defaultValue, it.location, nameNode.location);
}

fun buildVariableDeclaration(node: RawNode): VariableDeclarationNode {
    val nameNode = node.getSubNode("name").orElseThrow()
    val name = nameNode.value;
    val type = node.getSubNode("type_def").map { buildType(it.getSubNode("type").orElseThrow()) }.getOrNull();
    val value = node.getSubNode("assignment").map { buildExpression(it.getSubNode("expression").orElseThrow()) }
        .getOrNull();
    return VariableDeclarationNode(name, type, value, node.location, nameNode.location, keywordPos(node));
}

fun buildExpression(node: RawNode): ExpressionNode {
    val (type, value) = nodeType<ExpressionType>(node);
    return when (type) {
        ExpressionType.LITERAL_EXPRESSION -> buildLiteral(value)
        ExpressionType.STATEMENT_EXPRESSION -> StatementExpressionNode(buildStatement(value))
        ExpressionType.ENCLOSED_EXPRESSION -> buildExpression(value)
        ExpressionType.OPERATION_EXPRESSION -> buildOperation(value)
        ExpressionType.FIELD_ACCESS_EXPRESSION -> buildFieldAccess(value)
        ExpressionType.METHOD_INVOCATION -> StatementExpressionNode(buildMethodInvocation(value))
        ExpressionType.INDEXED_ACCESS_EXPRESSION -> buildIndexAccess(value);
    }
}

fun buildIndexAccess(value: RawNode): ExpressionNode {
    val array = buildExpression(value.getSubNode("array").orElseThrow());
    val index = buildExpression(value.getSubNode("index").orElseThrow());
    return IndexAccessNode(array, index, value.location);
}

fun buildFieldAccess(value: RawNode): FieldAccessNode {
    val target = buildExpression(value.getSubNode("target").orElseThrow());
    val fieldNode = value.getSubNode("field").orElseThrow()
    val field = fieldNode.value;
    return FieldAccessNode(target, field, value.location, fieldNode.location);
}

fun buildOperation(node: RawNode): OperationNode {
    val (type, rune) = nodeType<BiOperator>(node.getSubNode("operator").orElseThrow());
    val left = buildExpression(node.getSubNode("left").orElseThrow());
    val right = buildExpression(node.getSubNode("right").orElseThrow());
    return OperationNode(left, type, right, node.location, rune.location);
}

fun joinList(node: LocatableReducedNode): String {
    return node.children.joinToString("") { it.value };
}

fun buildLiteral(node: RawNode): ExpressionNode {
    val (type, value) = nodeType<LiteralType>(node);
    return when (type) {
        LiteralType.BOOLEAN_LITERAL -> BooleanLiteralNode(value.value.toBoolean(), node.location)
        LiteralType.STRING_LITERAL -> {
            val content = value.getSubNode("content").orElseThrow()
            StringLiteralNode(
                joinList(content),
                node.location,
                content.location
            )
        }

        LiteralType.NUMBER_LITERAL -> IntegerLiteralNode(value.value, node.location)
        LiteralType.VARIABLE_LITERAL -> VariableLiteralNode(value.value, node.location)
    }
}

fun buildType(it: RawNode): TypeNode {
    val (type, value) = nodeType<TypeDeclarationType>(it);
    return when (type) {
        TypeDeclarationType.ARRAY_TYPE -> buildArrayType(value)
        TypeDeclarationType.FIXED_VALUE_TYPE -> buildFixedValueType(value)
        TypeDeclarationType.NAMED_TYPE -> buildNamedType(value)
        TypeDeclarationType.UNION_TYPE -> buildUnionType(value)
        TypeDeclarationType.ENUM_TYPE -> buildEnumType(value)
    }
}

fun buildEnumType(value: RawNode): TypeNode {
    val nameNode = value.getSubNode("name")
    return EnumDeclarationNode(
        nameNode.map { it.value }.getOrNull(),
        value.getSubNode("constants").orElseThrow().children.map { EnumDeclarationNode.ConstantNode(it.value, it.location) },
        value.location,
        nameNode.map { it.location }.getOrNull(),
        keywordPos(value)
    );
}

fun buildArrayType(value: RawNode): ArrayTypeNode {
    val type = buildType(value.getSubNode("type").orElseThrow());
    val fixedSizeNode = value.getSubNode("fixed_size")
    val fixedSize = fixedSizeNode.map { it.value.toInt() }.getOrNull();
    return ArrayTypeNode(type, fixedSize, value.location, fixedSizeNode.getOrNull()?.location);
}

fun buildFixedValueType(value: RawNode): TypeNode {
    return TypeNode.ValueTypeNode(buildLiteral(value), value.location);
}

fun buildUnionType(node: RawNode): TypeNode {
    val types = node.children.map { buildType(it) }
    return TypeNode.UnionTypeNode(types, node.location);
}

fun buildNamedType(value: RawNode): TypeNode {
    val referencedTypeNameNode = value.getSubNode("referenced_type").orElseThrow()
    val name = referencedTypeNameNode.value;
    val generics = value.getSubNode("generic").map { it.getSubNode("types").orElseThrow() }
        .map {
            it.children.map { child -> buildType(child) }
        }.orElse(emptyList());
    return NamedTypeNode(name, generics, value.location, referencedTypeNameNode.location);
}

inline fun <reified T : Enum<T>> nodeType(it: RawNode, postfix: UnaryOperator<String>? = null): Pair<T, RawNode> {
    val typeString = when (postfix) {
        null -> it.getSubNode("type").orElseThrow().value
        else -> postfix.apply(it.getSubNode("type").orElseThrow().value)
    }
    val type = enumValues<T>().find { e -> e.name == typeString }
        ?: throw IllegalStateException("Unknown '${typeOf<T>()}': $typeString");
    val value = it.getSubNode("value").orElseThrow();
    return Pair(type, value);
}
