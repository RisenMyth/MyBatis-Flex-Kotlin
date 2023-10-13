package internal.gen.tables

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import internal.config.flex.AllInTablesClassName
import internal.config.flex.AllInTablesPackage
import internal.util.suppressDefault
import internal.util.write
import logger


class TablesGenerator : () -> Unit {
    companion object {
        private var isExists = false  // 此属性用于判断是否已经生成了 Tables 类。
    }

    val properties = ArrayList<PropertySpec>()

    override fun invoke() {
        generate()
    }

    private fun generate() {
        // 到这一步时，AllInTablesPackage.value 已确定非空。这里的可空性已由 MybatisFlexKSP 中进行判断。
        val packageName = AllInTablesPackage.value ?: return logger.warn("指定了生成类 Tables 但没有指定生成在哪个包下，不予生成。")
        if (isExists) return logger.warn("Tables has exists.")
        val fileSpec = FileSpec.builder(packageName, AllInTablesClassName.value)
            .addType(
                TypeSpec.objectBuilder(AllInTablesClassName.value)
                    .addKdoc(
                        """
                |This file is automatically generated by the ksp of mybatis-flex, do not modify this file.
                """.trimMargin()
                    )
                    .addProperties(properties)
                    .build()
            )
            .suppressDefault()
            .build()
        fileSpec.write()
        isExists = true
    }
}