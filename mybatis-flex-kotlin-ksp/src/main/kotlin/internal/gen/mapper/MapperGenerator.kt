package com.mybatisflex.kotlin.ksp.internal.gen.mapper

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.mybatisflex.kotlin.ksp.internal.config.flex.MapperAnnotation
import com.mybatisflex.kotlin.ksp.internal.config.flex.MapperBaseClass
import com.mybatisflex.kotlin.ksp.internal.util.*
import com.mybatisflex.kotlin.ksp.logger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import org.apache.ibatis.annotations.Mapper
import java.nio.charset.Charset


internal class MapperGenerator(private val mapper: KSClassDeclaration) {
    private val superTypeParam = mapper.superTypes.first().element?.typeArguments?.firstOrNull()

    /**
     * 执行此方法后，立即生成 Mapper 类。
     * @param classDeclaration 要生成 Mapper 的类声明。
     * @author CloudPlayer
     */
    operator fun invoke(classDeclaration: KSClassDeclaration) {
        if (MapperBaseClass.isOriginalBaseMapper || mapper.toClassName() == BASE_MAPPER) {
            return generateMapper(classDeclaration, BASE_MAPPER.plusParameter(classDeclaration.toClassName()))
        }
        generateTypedMapper(classDeclaration)
    }

    /**
     * 生成 Mapper，仅适用于没有泛型或泛型只有一个的情形。
     *
     * @param classDeclaration 要生成 Mapper 的实体类。
     * @param className Mapper 要继承的接口。如果接口有一个泛型，那么该泛型参数将是实体类的类型。
     */
    private fun generateMapper(classDeclaration: KSClassDeclaration, className: TypeName) {
        val interfaceName = classDeclaration.interfaceName
        val fileSpec = FileSpec.builder(classDeclaration.mapperPackageName, interfaceName)
        fileSpec.addType(
            TypeSpec.interfaceBuilder(
                ClassName(classDeclaration.mapperPackageName, interfaceName)
            ).apply {
                addSuperinterface(className)
                addKdoc("""Mapper for [${classDeclaration.simpleName.asString()}]""")
                if (MapperAnnotation.value) {
                    addAnnotation(Mapper::class)
                }
            }.build()
        )
            .suppressDefault()
            .build()
            .write()
    }

    /**
     * 生成 Mapper，适用于有泛型的情形。
     * @param classDeclaration 对应的泛型，即需要生成 Mapper 的实体类。
     */

    private fun generateTypedMapper(classDeclaration: KSClassDeclaration) {
        val typeParams = mapper.typeParameters
        if (typeParams.isEmpty()) {
            generateMapper(classDeclaration, mapper.toClassName())
        }
        if (typeParams.size == 1) {
            val typeParam = typeParams[0]
            val thisType = typeParam.name.asString()  // 获取自定义父类的泛型名称。
            // 获取父类在继承 BaseMapper 时使用的泛型。列表中的第一个元素表示其为逆变。
            val superTypeParam = superTypeParam ?: kotlin.run {
                val exception =
                    IllegalArgumentException("Custom Mapper parent classes must inherit from $BASE_MAPPER, not ${mapper.toClassName()}")
                logger.exception(exception)
                throw IllegalArgumentException()
            }
            val superType = superTypeParam.toString().split(" ")[1]
            if (thisType == superType) {
                generateMapper(classDeclaration, mapper.toClassName().plusParameter(classDeclaration.toClassName()))
            } else {
                logger.exception(
                    UnsupportedOperationException(
                        """
                    |要想在自定义的 Mapper 中使用泛型，你需要保证此 Mapper 直接继承至 BaseMapper ，此外
                    |你需要保证自定义的 Mapper 中携带的泛型直接传递至 BaseMapper 中的泛型，且泛型名称一致。
                    |你在自定义 Mapper 中携带的泛型: $thisType，你在继承 BaseMapper 时使用的泛型: $superType
                    |
                    |你的代码:
                    |       $mapper<$thisType> : BaseMapper<$superType>
                    |你需要让代码看起来像这样，让二者的泛型一致:
                    |       $mapper<$thisType> : BaseMapper<$thisType>
                """.toByteArray(Charsets.UTF_8).toString(Charset.defaultCharset()).trimMargin()
                    )
                )
            }
        } else logger.exception(UnsupportedOperationException("自定义 Mapper 泛型尚不支持多于一个的泛型。"))
    }
}