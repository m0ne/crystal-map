package com.schwarz.crystalprocessor.model.field

import com.schwarz.crystalprocessor.model.deprecated.DeprecatedModel
import com.schwarz.crystalprocessor.util.ConversionUtil
import com.schwarz.crystalprocessor.util.FieldExtractionUtil
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.schwarz.crystalapi.Field
import org.apache.commons.lang3.text.WordUtils
import javax.lang.model.type.TypeMirror

/**
 * Created by sbra0902 on 21.06.17.
 */

abstract class CblBaseFieldHolder(val dbField: String, private val mField: Field) {

    val typeMirror: TypeMirror = FieldExtractionUtil.typeMirror(mField)

    open val isIterable: Boolean
        get() = mField.list

    val isDefault: Boolean
        get() = !mField.defaultValue.isEmpty() && !isConstant

    val isConstant: Boolean
        get() = mField.readonly

    val constantName: String
        get() = ConversionUtil.convertCamelToUnderscore(dbField).toUpperCase()

    val defaultValue: String
        get() = mField.defaultValue

    val comment: Array<String>
        get() = mField.comment

    abstract val fieldType: TypeName

    fun accessorSuffix(): String {
        return WordUtils.uncapitalize(
            WordUtils.capitalize(dbField.replace("_".toRegex(), " ")).replace(" ".toRegex(), "")
        )
    }

    abstract fun interfaceProperty(isOverride: Boolean = false, deprecated: DeprecatedModel?): PropertySpec

    abstract fun property(
        dbName: String?,
        possibleOverrides: Set<String>,
        useMDocChanges: Boolean,
        deprecated: DeprecatedModel?
    ): PropertySpec

    abstract fun builderSetter(
        dbName: String?,
        packageName: String,
        entitySimpleName: String,
        useMDocChanges: Boolean,
        deprecated: DeprecatedModel?
    ): FunSpec?

    abstract fun createFieldConstant(): List<PropertySpec>
}
