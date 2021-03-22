package com.kaufland.model.mapper.type

import com.kaufland.ProcessingContext
import com.kaufland.ProcessingContext.asDeclaringName
import com.kaufland.javaToKotlinType
import com.squareup.kotlinpoet.*
import kaufland.com.coachbasebinderapi.mapify.Mapify
import java.lang.reflect.Field
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

class MapifyElementTypeField(val element: Element, val mapify: Mapify) : MapifyElementType {

    override val fieldName = element.simpleName.toString()

    override val mapName = if (mapify.name.isNotBlank()) mapify.name else fieldName

    override val typeName = element.asType().asTypeName().javaToKotlinType()

    override val accessible = element.modifiers.contains(Modifier.PUBLIC)

    override val declaringName: ProcessingContext.DeclaringName = element.asDeclaringName()

    override fun reflectionProperties(sourceClazzTypeName: TypeName): List<PropertySpec> {
        return listOf(PropertySpec.builder(reflectedFieldName, Field::class.java.asTypeName(), KModifier.PRIVATE)
                .initializer(CodeBlock.builder()
                        .addStatement("%T::class.java.getDeclaredField(%S)", sourceClazzTypeName, fieldName)
                        .beginControlFlow(".apply")
                        .addStatement("isAccessible·=·true")
                        .endControlFlow().build()).build())
    }

    override fun getterFunSpec(): FunSpec {
        return FunSpec.getterBuilder().addStatement("return %N.get(this) as? %T", reflectedFieldName, typeName.copy(nullable = true)).build()
    }

    override fun setterFunSpec(): FunSpec {
        return FunSpec.setterBuilder().addParameter("value", typeName).addStatement("%N.set(this,·value)", reflectedFieldName).build()
    }
}