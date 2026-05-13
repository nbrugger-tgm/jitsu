package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.parser.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import kotlinx.serialization.SerialName

sealed interface StatementNode : AstNode {

    sealed interface InstructionNode : StatementNode {
        class FunctionCallNode(
            val function: IdentifierNode,
            val parameters: List<ExpressionNode>,
            override val location: Location
        ) : AstNodeImpl(), InstructionNode, ExpressionNode {


            override val children: List<AstNode>
                get() = parameters + function
        }

        class AssignmentNode(
            val target: AssignmentTarget,
            val value: ExpressionNode?,
        ) : InstructionNode, AstNodeImpl() {
            override val children: List<AstNode>
                get() = listOfNotNull(target, value)

            sealed interface AssignmentTarget : AstNode

            override val location: Location
                get() = target.location.rangeTo(value?.location ?: target.location)
        }

        class MethodInvocationNode(
            val method: ExpressionNode.FieldAccessNode,
            val parameters: List<ExpressionNode>,
            override val location: Location
        ) : InstructionNode, ExpressionNode, AstNodeImpl() {


            override val children: List<AstNode>
                get() = parameters + method
        }

        data class LineCommentNode(val content: Located<String>, override val location: Location) : InstructionNode,
            AstNodeImpl() {


            override val children: List<AstNode>
                get() = listOf()
        }


        data class YieldStatement(
            val expression: ExpressionNode?,
            val keywordLocation: Location
        ) : InstructionNode, AstNodeImpl() {
            override val children: List<AstNode>
                get() = listOfNotNull(expression)
            override val location: Location
                get() = if (expression == null) keywordLocation else keywordLocation.rangeTo(expression.location)
        }

        class SwitchNode(
            val item: ExpressionNode?,
            val cases: List<CaseNode>,
            override val location: Location,
            val keywordLocation: Location
        ) : AstNodeImpl(), InstructionNode, ExpressionNode {
            override val children: List<AstNode>
                get() = cases + listOfNotNull(item)

            abstract class CaseNode(
                val matcher: CaseMatchNode,
                val body: CaseBodyNode?,
                val keywordLocation: Location
            ) :
                AstNode {
                sealed interface CaseMatchNode : AstNode {
                    class ConstantCaseNode(
                        val value: ExpressionNode,
                        val keywordLocation: Location,
                        override val location: Location
                    ) : CaseMatchNode,
                        AstNodeImpl() {


                        override val children: List<AstNode>
                            get() = listOf(value)
                    }

                    class TypeCaseNode(
                        @SerialName("type_definition")
                        val type: TypeNode.NameTypeNode,
                        val keywordLocation: Location,
                        override val location: Location
                    ) : CaseMatchNode, AstNodeImpl() {


                        override val children: List<AstNode>
                            get() = listOf(type)
                    }

                    class DefaultCaseNode(override val location: Location) : CaseMatchNode, AstNodeImpl()

                    override val children: List<AstNode>
                        get() = listOf()
                }

                sealed interface CaseBodyNode : AstNode
            }
        }

        sealed interface CodeBlockNode : InstructionNode, ExpressionNode,
            SwitchNode.CaseNode.CaseBodyNode,
            Declaration.FunctionDeclarationNode.FunctionBodyNode {
            class SingleExpressionCodeBlock(val expression: ExpressionNode, override val location: Location) :
                AstNodeImpl(), CodeBlockNode {
                override val children: List<AstNode>
                    get() = listOf(expression)
            }

            class StatementsCodeBlock(val statements: List<StatementNode>, override val location: Location) :
                AstNodeImpl(), CodeBlockNode {

                override val children: List<AstNode>
                    get() = statements
            }
        }

        class IfNode(
            val condition: ExpressionNode?,
            val thenCodeBlockNode: CodeBlockNode?,
            val elseStatement: ElseNode?,
            override val location: Location,
            val keywordLocation: Location
        ) : AstNodeImpl(), InstructionNode, ExpressionNode {
            override val children: List<AstNode>
                get() = listOfNotNull(condition, thenCodeBlockNode, elseStatement)

            sealed interface ElseNode : AstNode {
                val keywordLocation: Location

                class ElseIfNode(val ifNode: IfNode, override val keywordLocation: Location) : AstNodeImpl(),
                    ElseNode {
                    override val children: List<AstNode>
                        get() = listOf(ifNode)
                    override val location: Location
                        get() = keywordLocation.rangeTo(ifNode.location)
                }

                class ElseBlockNode(val codeBlock: CodeBlockNode?, override val keywordLocation: Location) :
                    AstNodeImpl(), ElseNode {
                    override val children: List<AstNode>
                        get() = listOfNotNull(codeBlock)
                    override val location: Location
                        get() = if (codeBlock == null) keywordLocation else keywordLocation.rangeTo(codeBlock.location)
                }
            }
        }

        class ReturnNode(
            val expression: ExpressionNode?,
            override val location: Location,
            val keywordLocation: Location
        ) :
            AstNodeImpl(), InstructionNode {


            override val children: List<AstNode>
                get() = listOfNotNull(expression)
        }

        class VariableDeclarationNode(
            val name: IdentifierNode?,
            @SerialName("type_definition")
            val type: TypeNode?,
            val value: ExpressionNode?,
            val keywordLocation: Location
        ) : AstNodeImpl(), InstructionNode, Declaration {
            override val location: Location
                get() = keywordLocation.rangeTo(
                    value?.location ?: type?.location ?: keywordLocation
                )
            override val children: List<AstNode>
                get() = listOfNotNull(name, type, value)
        }
    }

    sealed interface Declaration : StatementNode {
        class FunctionDeclarationNode(
            val name: IdentifierNode?,
            val parameters: List<ParameterNode>,
            val returnType: TypeNode?,
            val body: FunctionBodyNode?,
            val keywordLocation: Location,
            override val attributes: List<AttributeNode>
        ) : AstNodeImpl(), Declaration, InstructionNode, ExpressionNode,
            CanHaveAttributes {
            override val children: List<AstNode>
                get() = parameters + listOfNotNull(name, returnType, body) + attributes
            override val location: Location = keywordLocation.rangeTo(
                body?.location ?: returnType?.location ?: parameters.lastOrNull()?.location ?: name?.location
                ?: keywordLocation
            )

            sealed interface FunctionBodyNode : AstNode {
                data class NativeImplementation(override val location: Location) : FunctionBodyNode, AstNodeImpl() {
                    override val children: List<AstNode> get() = emptyList()
                }
            }

            class ParameterNode(
                val name: IdentifierNode,
                val type: TypeNode?,
                val defaultValue: ExpressionNode?,
            ) : AstNodeImpl() {
                override fun toString() = "${name.value} : $type${if (defaultValue != null) " = $defaultValue" else ""}"
                override val location: Location =
                    name.location.rangeTo(defaultValue?.location ?: type?.location ?: name.location)
                override val children: List<AstNode>
                    get() = listOfNotNull(name, type, defaultValue)
            }

        }

        data class ImportNode(val kw: Location, val moduleReference: Located<String>) : AstNodeImpl(), Declaration {
            override val location: Location get() = kw.rangeTo(moduleReference.location.end)
            override val children: List<AstNode> get() = listOf()
        }
    }

    sealed interface NamedTypeDeclarationNode : AstNode, Declaration, CanHaveAttributes {
        val name: IdentifierNode?;

        data class TypeAliasNode(
            override val name: IdentifierNode?,
            val typeParameters: List<IdentifierNode>,
            @SerialName("definition") val type: TypeNode?,
            override val location: Location,
            val keywordLocation: Location,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl() {
            override val children: List<AstNode>
                get() = attributes + listOfNotNull(name, type)
        }

        data class ClassDeclarationNode(
            override val name: IdentifierNode?,
            val typeParameters: List<IdentifierNode>,
            val fields: List<StructuralFieldNode>,
            val methods: List<MethodNode>,
            override val location: Location,
            val keywordLocation: Location,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl(), CanHaveAttributes {
            override val children: List<AstNode>
                get() = attributes + methods + fields + listOfNotNull(name) + typeParameters

            data class MethodNode(val function: Declaration.FunctionDeclarationNode, val mutableKw: Location?) :
                AstNodeImpl() {
                override val location: Location get() = mutableKw?.rangeTo(function.location) ?: function.location
                override val children: List<AstNode> get() = listOf(function)
            }
        }

        data class EnumDeclarationNode(
            override val name: IdentifierNode,
            val constants: List<IdentifierNode>,
            override val location: Location,
            val keywordLocation: Location,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl() {
            override val children: List<AstNode>
                get() = constants + name

            override fun toString(): String {
                return "enum {${constants.joinToString(", ") { it.value }}}"
            }
        }

        data class InterfaceTypeNode(
            override val name: IdentifierNode,
            val functions: List<FunctionSignatureNode>,
            override val location: Location,
            val keywordLocation: Location,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl(), CanHaveAttributes {
            override val children: List<AstNode>
                get() = functions + attributes + name

            override fun toString(): String {
                return name.value
            }

            class FunctionSignatureNode(
                val name: IdentifierNode,
                val typeSignature: TypeNode.FunctionTypeSignatureNode,
            ) : AstNodeImpl() {
                override val location: Location get() = name.location.rangeTo(typeSignature.location)
                override val children: List<AstNode>
                    get() = listOf(name, typeSignature)
            }

        }
    }
}
