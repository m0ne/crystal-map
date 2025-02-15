package com.schwarz.crystalprocessor.model.source

import com.schwarz.crystalprocessor.Logger
import com.schwarz.crystalprocessor.javaToKotlinType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import com.schwarz.crystalapi.BasedOn
import com.schwarz.crystalapi.Comment
import com.schwarz.crystalapi.DocId
import com.schwarz.crystalapi.DocIdSegment
import com.schwarz.crystalapi.Entity
import com.schwarz.crystalapi.Field
import com.schwarz.crystalapi.Fields
import com.schwarz.crystalapi.GenerateAccessor
import com.schwarz.crystalapi.MapWrapper
import com.schwarz.crystalapi.Reduce
import com.schwarz.crystalapi.Reduces
import com.schwarz.crystalapi.deprecated.Deprecated
import com.schwarz.crystalapi.query.Queries
import com.schwarz.crystalapi.query.Query
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import org.apache.commons.lang3.text.WordUtils
import org.jetbrains.annotations.Nullable
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import kotlin.coroutines.Continuation

@OptIn(DelicateKotlinPoetApi::class)
data class SourceModel(private val sourceElement: Element) : ISourceModel, IClassModel {

    override val sourceClazzSimpleName: String =
        (sourceElement as Symbol.ClassSymbol).simpleName.toString()
    override val sourcePackage: String = (sourceElement as Symbol.ClassSymbol).packge().toString()

    override val sourceClazzTypeName: TypeName = ClassName(sourcePackage, sourceClazzSimpleName)

    override val entityAnnotation: Entity? = sourceElement.getAnnotation(Entity::class.java)
    override val typeName: TypeName = sourceElement.asType().asTypeName()
    override val mapWrapperAnnotation: MapWrapper? =
        sourceElement.getAnnotation(MapWrapper::class.java)
    override val commentAnnotation: Comment? = sourceElement.getAnnotation(Comment::class.java)
    override val deprecatedAnnotation: Deprecated? =
        sourceElement.getAnnotation(Deprecated::class.java)
    override val docIdAnnotation: DocId? = sourceElement.getAnnotation(DocId::class.java)
    override val basedOnAnnotation: BasedOn? = sourceElement.getAnnotation(BasedOn::class.java)

    override val reduceAnnotations: List<Reduce> =
        sourceElement.getAnnotation(Reduces::class.java)?.value?.toList() ?: emptyList()
    override val fieldAnnotations: List<Field> =
        sourceElement.getAnnotation(Fields::class.java)?.value?.toList() ?: emptyList()
    override val queryAnnotations: List<Query> =
        sourceElement.getAnnotation(Queries::class.java)?.value?.toList() ?: emptyList()

    override val abstractParts: Set<String>

    override fun logError(logger: Logger, message: String) {
        logger.error(message, sourceElement)
    }

    override val relevantStaticFunctions: List<SourceMemberFunction>

    override val relevantStaticFields: List<SourceMemberField>

    init {
        abstractParts = findPossibleOverrides(sourceElement)
        val relevantStaticsFields = mutableListOf<SourceMemberField>()
        val relevantStaticsFunctions = mutableListOf<SourceMemberFunction>()
        parseStaticsFromStructure(sourceElement) {

            val accessor = it.getAnnotation(GenerateAccessor::class.java)
            val docSegment = it.getAnnotation(DocIdSegment::class.java)

            if (accessor != null || docSegment != null) {

                when (it.kind) {
                    ElementKind.FIELD -> {
                        relevantStaticsFields.add(
                            SourceMemberField(
                                it.simpleName.toString(),
                                evaluateTypeName(
                                    it.asType(),
                                    it.getAnnotation(Nullable::class.java) != null
                                ),
                                docSegment,
                                accessor
                            )
                        )
                    }
                    ElementKind.METHOD -> {
                        (it as? ExecutableElement)?.let {
                            var isSuspend = false
                            val parameter = mutableListOf<Parameter>()
                            it.parameters.forEach {
                                if (isSuspendFunction(it)) {
                                    isSuspend = true
                                } else {
                                    parameter.add(
                                        Parameter(
                                            it.simpleName.toString(),
                                            evaluateTypeName(
                                                it.asType(),
                                                it.getAnnotation(Nullable::class.java) != null
                                            )
                                        )
                                    )
                                }
                            }

                            relevantStaticsFunctions.add(
                                SourceMemberFunction(
                                    name = it.simpleName.toString(),
                                    isSuspend = isSuspend,
                                    parameters = parameter,
                                    generateAccessor = accessor,
                                    docIdSegment = docSegment,
                                    returnTypeName = it.returnType.asTypeName().javaToKotlinType().copy(it.getAnnotation(Nullable::class.java) != null)
                                )
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
        relevantStaticFunctions = relevantStaticsFunctions
        relevantStaticFields = relevantStaticsFields
    }

    private fun isSuspendFunction(varElement: VariableElement): Boolean {
        return varElement.asType().toString().contains(Continuation::class.qualifiedName.toString())
    }

    private fun evaluateTypeName(typeMirror: TypeMirror, nullable: Boolean): TypeName {
        return typeMirror.asTypeName().javaToKotlinType().copy(nullable = nullable)
    }

    private fun parseStaticsFromStructure(cblEntityElement: Element, mapper: (Element) -> Unit) {
        for (childElement in cblEntityElement.enclosedElements) {
            if (childElement.modifiers.contains(Modifier.STATIC)) {
                if (childElement.kind == ElementKind.CLASS && childElement.simpleName.toString() == "Companion") {
                    for (companionMembers in childElement.enclosedElements) {
                        mapper.invoke(companionMembers)
                    }
                    continue
                }
                mapper.invoke(childElement)
            }
        }
    }

    private fun findPossibleOverrides(cblEntityElement: Element): HashSet<String> {
        var abstractSet = HashSet<String>()
        for (enclosedElement in cblEntityElement.enclosedElements) {
            if (enclosedElement.modifiers.contains(Modifier.ABSTRACT) && (enclosedElement.kind == ElementKind.FIELD || enclosedElement.kind == ElementKind.METHOD)) {
                var name = enclosedElement.simpleName.toString()
                if (name.startsWith("set")) {
                    abstractSet.add(WordUtils.uncapitalize(name.replace("set", "")))
                } else if (name.startsWith("get")) {
                    abstractSet.add(WordUtils.uncapitalize(name.replace("get", "")))
                } else {
                    abstractSet.add(name)
                }
            }
        }
        return abstractSet
    }
}
