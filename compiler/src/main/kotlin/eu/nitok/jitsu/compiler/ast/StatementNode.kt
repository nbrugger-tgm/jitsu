package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface StatementNode : AstNode {

    @Serializable
    sealed interface InstructionNode : StatementNode {
        @Serializable
        class FunctionCallNode(
            val function: IdentifierNode,
            val parameters: List<ExpressionNode>,
            override val location: Range
        ) : AstNodeImpl(), InstructionNode, ExpressionNode {


            override val children: List<AstNode>
                get() = parameters + function
        }

        @Serializable
        class AssignmentNode(
            val target: AssignmentTarget,
            val value: ExpressionNode?,
        ) : InstructionNode, AstNodeImpl() {
            override val children: List<AstNode>
                get() = listOfNotNull(target, value)

            @Serializable
            sealed interface AssignmentTarget : AstNode

            override val location: Range
                get() = target.location.rangeTo(value?.location ?: target.location)
        }

        @Serializable
        class MethodInvocationNode(
            val method: ExpressionNode.FieldAccessNode,
            val parameters: List<ExpressionNode>,
            override val location: Range
        ) : InstructionNode, ExpressionNode, AstNodeImpl() {


            override val children: List<AstNode>
                get() = parameters + method
        }

        @Serializable
        data class LineCommentNode(val content: Located<String>, override val location: Range) : InstructionNode,
            AstNodeImpl() {


            override val children: List<AstNode>
                get() = listOf()
        }


        @Serializable
        data class YieldStatement(
            val expression: ExpressionNode?,
            val keywordLocation: Range
        ) : InstructionNode, AstNodeImpl() {
            override val children: List<AstNode>
                get() = listOfNotNull(expression)
            override val location: Range
                get() = if (expression == null) keywordLocation else keywordLocation.rangeTo(expression.location)
        }

        @Serializable
        class SwitchNode(
            val item: ExpressionNode?,
            val cases: List<CaseNode>,
            override val location: Range,
            val keywordLocation: Range
        ) : AstNodeImpl(), InstructionNode, ExpressionNode {
            override val children: List<AstNode>
                get() = cases + listOfNotNull(item)

            @Serializable
            abstract class CaseNode(val matcher: CaseMatchNode, val body: CaseBodyNode?, val keywordLocation: Range) :
                AstNode {
                @Serializable
                sealed interface CaseMatchNode : AstNode {
                    @Serializable
                    class ConstantCaseNode(
                        val value: ExpressionNode,
                        val keywordLocation: Range,
                        override val location: Range
                    ) : CaseMatchNode,
                        AstNodeImpl() {


                        override val children: List<AstNode>
                            get() = listOf(value)
                    }

                    @Serializable
                    class TypeCaseNode(
                        @SerialName("type_definition")
                        val type: TypeNode.NameTypeNode,
                        val keywordLocation: Range,
                        override val location: Range
                    ) : CaseMatchNode, AstNodeImpl() {


                        override val children: List<AstNode>
                            get() = listOf(type)
                    }

                    @Serializable
                    class DefaultCaseNode(override val location: Range) : CaseMatchNode, AstNodeImpl()

                    override val children: List<AstNode>
                        get() = listOf()
                }

                @Serializable
                sealed interface CaseBodyNode : AstNode
            }
        }

        @Serializable
        sealed interface CodeBlockNode : InstructionNode, ExpressionNode, SwitchNode.CaseNode.CaseBodyNode {
            @Serializable
            class SingleExpressionCodeBlock(val expression: ExpressionNode, override val location: Range) :
                AstNodeImpl(), CodeBlockNode {
                override val children: List<AstNode>
                    get() = listOf(expression)
            }

            @Serializable
            class StatementsCodeBlock(val statements: List<StatementNode>, override val location: Range) :
                AstNodeImpl(), CodeBlockNode {

                override val children: List<AstNode>
                    get() = statements
            }
        }

        @Serializable
        class IfNode(
            val condition: ExpressionNode?,
            val thenCodeBlockNode: CodeBlockNode?,
            val elseStatement: ElseNode?,
            override val location: Range,
            val keywordLocation: Range
        ) : AstNodeImpl(), InstructionNode, ExpressionNode {
            override val children: List<AstNode>
                get() = listOfNotNull(condition, thenCodeBlockNode, elseStatement)

            @Serializable
            sealed interface ElseNode : AstNode {
                val keywordLocation: Range

                @Serializable
                class ElseIfNode(val ifNode: IfNode, override val keywordLocation: Range) : AstNodeImpl(),
                    ElseNode {
                    override val children: List<AstNode>
                        get() = listOf(ifNode)
                    override val location: Range
                        get() = keywordLocation.rangeTo(ifNode.location)
                }

                @Serializable
                class ElseBlockNode(val codeBlock: CodeBlockNode?, override val keywordLocation: Range) :
                    AstNodeImpl(), ElseNode {
                    override val children: List<AstNode>
                        get() = listOfNotNull(codeBlock)
                    override val location: Range
                        get() = if (codeBlock == null) keywordLocation else keywordLocation.rangeTo(codeBlock.location)
                }
            }
        }

        @Serializable
        class ReturnNode(val expression: ExpressionNode?, override val location: Range, val keywordLocation: Range) :
            AstNodeImpl(), InstructionNode {


            override val children: List<AstNode>
                get() = listOfNotNull(expression)
        }

        @Serializable
        class VariableDeclarationNode(
            val name: IdentifierNode?,
            @SerialName("type_definition")
            val type: TypeNode?,
            val value: ExpressionNode?,
            val keywordLocation: Range
        ) : AstNodeImpl(), InstructionNode, Declaration {
            override val location: Range
                get() = keywordLocation.rangeTo(
                    value?.location ?: type?.location ?: keywordLocation
                )
            override val children: List<AstNode>
                get() = listOfNotNull(name, type, value)
        }
    }

    @Serializable
    sealed interface Declaration : StatementNode {
        @Serializable
        class FunctionDeclarationNode(
            val name: IdentifierNode?,
            val parameters: List<ParameterNode>,
            val returnType: TypeNode?,
            val body: InstructionNode.CodeBlockNode?,
            val keywordLocation: Range,
            override val attributes: List<AttributeNode>
        ) : AstNodeImpl(), Declaration, InstructionNode, ExpressionNode,
            CanHaveAttributes {
            override val children: List<AstNode>
                get() = parameters + listOfNotNull(name, returnType, body) + attributes
            override val location: Range = keywordLocation.rangeTo(
                body?.location ?: returnType?.location ?: parameters.lastOrNull()?.location ?: name?.location
                ?: keywordLocation
            )

            @Serializable
            class ParameterNode(
                val name: IdentifierNode,
                val type: TypeNode?,
                val defaultValue: ExpressionNode?,
            ) : AstNodeImpl() {
                override fun toString() = "${name.value} : $type${if (defaultValue != null) " = $defaultValue" else ""}"
                override val location: Range =
                    name.location.rangeTo(defaultValue?.location ?: type?.location ?: name.location)
                override val children: List<AstNode>
                    get() = listOfNotNull(name, type, defaultValue)
            }

        }
    }


    //    @Serializabledata
    //    class While(val condition: ExpressionNode, val body: CodeBlockNode) : StatementNode()
//    @Serializabledata
//    class For(val variable: VariableDeclaration, val condition: ExpressionNode, val increment: ExpressionNode, val body: CodeBlockNode) : StatementNode()
//    @Serializabledata
//    class Break(val label: String?) : StatementNode()
//    @Serializabledata
//    class Continue(val label: String?) : StatementNode()
//    @Serializabledata
//    class Label(val label: String) : StatementNode()


    sealed interface NamedTypeDeclarationNode : AstNode, Declaration {
        val name: IdentifierNode?;

        @Serializable
        data class TypeAliasNode(
            override val name: IdentifierNode?,
            val typeParameters: List<IdentifierNode>,
            @SerialName("definition") val type: TypeNode?,
            override val location: Range,
            val keywordLocation: Range,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl(), CanHaveAttributes {
            override val children: List<AstNode>
                get() = attributes + listOfNotNull(name, type)
        }

        @Serializable
        data class ClassDeclarationNode(
            override val name: IdentifierNode?,
            val typeParameters: List<IdentifierNode>,
            val fields: List<StructuralFieldNode>,
            val methods: List<MethodNode>,
            override val location: Range,
            val keywordLocation: Range,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl(), CanHaveAttributes {
            override val children: List<AstNode>
                get() = attributes + methods + fields + listOfNotNull(name) + typeParameters

            @Serializable
            data class MethodNode(val function: Declaration.FunctionDeclarationNode, val mutableKw: Range?) :
                AstNodeImpl() {
                override val location: Range get() = mutableKw?.rangeTo(function.location) ?: function.location
                override val children: List<AstNode> get() = listOf(function)
            }
        }

        @Serializable
        data class EnumDeclarationNode(
            override val name: IdentifierNode,
            val constants: List<IdentifierNode>,
            override val location: Range,
            val keywordLocation: Range
        ) : NamedTypeDeclarationNode, AstNodeImpl() {
            override val children: List<AstNode>
                get() = constants + name

            override fun toString(): String {
                return "enum {${constants.joinToString(", ") { it.value }}}"
            }
        }

        @Serializable
        data class InterfaceTypeNode(
            override val name: IdentifierNode,
            val functions: List<FunctionSignatureNode>,
            override val location: Range,
            val keywordLocation: Range,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl(), CanHaveAttributes {
            override val children: List<AstNode>
                get() = functions + attributes + name

            override fun toString(): String {
                return name.value
            }

            @Serializable
            class FunctionSignatureNode(
                val name: IdentifierNode,
                val typeSignature: TypeNode.FunctionTypeSignatureNode,
            ) : AstNodeImpl() {
                override val location: Range get() = name.location.rangeTo(typeSignature.location)
                override val children: List<AstNode>
                    get() = listOf(name, typeSignature)
            }

        }
    }
}