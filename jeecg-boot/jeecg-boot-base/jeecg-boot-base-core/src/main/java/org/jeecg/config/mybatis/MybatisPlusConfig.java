package org.jeecg.config.mybatis;

import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.ISqlParserFilter;
import com.baomidou.mybatisplus.core.parser.SqlParserHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.tenant.TenantHandler;
import com.baomidou.mybatisplus.extension.plugins.tenant.TenantSqlParser;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.jeecg.common.util.oConvertUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 单数据源配置（jeecg.datasource.open = false时生效）
 *
 * @Author zhoujf
 */
@Configuration
@Slf4j
@MapperScan(value = {"org.jeecg.modules.**.mapper*"})
public class MybatisPlusConfig {

    /**
     * tenant_id 字段名
     */
    public static final String tenant_field = "tenant_id";

    /**
     * 有哪些表需要做多租户 这些表需要添加一个字段 ，字段名和tenant_field对应的值一样
     */
    private static final List<String> tenantTable = new ArrayList<String>();
    /**
     * ddl 关键字 判断不走多租户的sql过滤
     * 样例全路径：vip.mate.system.mapper.UserMapper.findList
     */
    private static final List<String> ignoreSqls = new ArrayList<String>();

    static {
        // -------------------------------------- 需要开启多租户的数据库表名 -----------------------------------------
        // tenantTable.add("jee_bug_danbiao");
        // -------------------------------------- 需要排除多租户的 mapper 方法 --------------------------------------
        // ignoreSqls.add("org.jeecg.modules.shop.mapper.GoodsOrderMapper.selectPageByShopId");
        ignoreSqls.add("alter");
    }

    /**
     * 多租户属于 SQL 解析部分，依赖 MP 分页插件
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor().setLimit(-1);
        //多租户配置 配置后每次执行sql会走一遍他的转化器 如果不需要多租户功能 可以将其注释
        tenantConfig(paginationInterceptor);
        return paginationInterceptor;
    }

    /**
     * 多租户的配置
     *
     * @param paginationInterceptor
     */
    private void tenantConfig(PaginationInterceptor paginationInterceptor) {
        /*
         * 【测试多租户】 SQL 解析处理拦截器<br>
         * 这里固定写成住户 1 实际情况你可以从cookie读取，因此数据看不到 【 麻花藤 】 这条记录（ 注意观察 SQL ）<br>
         */
        List<ISqlParser> sqlParserList = new ArrayList<>();
        TenantSqlParser tenantSqlParser = new JeecgTenantParser();
        tenantSqlParser.setTenantHandler(new TenantHandler() {

            @Override
            public Expression getTenantId(boolean select) {
                String tenantId = oConvertUtils.getString(TenantContext.getTenant(), "-1");
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return tenant_field;
            }

            @Override
            public boolean doTableFilter(String tableName) {
                //true则不加租户条件查询  false则加
                // return excludeTable.contains(tableName);
                return !tenantTable.contains(tableName);
            }

            private Expression in(String ids) {
                final InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(new Column(getTenantIdColumn()));
                final ExpressionList itemsList = new ExpressionList();
                final List<Expression> inValues = new ArrayList<>(2);
                for (String id : ids.split(",")) {
                    inValues.add(new LongValue(id));
                }
                itemsList.setExpressions(inValues);
                inExpression.setRightItemsList(itemsList);
                return inExpression;
            }

        });

        sqlParserList.add(tenantSqlParser);
        paginationInterceptor.setSqlParserList(sqlParserList);
        paginationInterceptor.setSqlParserFilter(new ISqlParserFilter() {
            @Override
            public boolean doFilter(MetaObject metaObject) {
                MappedStatement ms = SqlParserHelper.getMappedStatement(metaObject);
                String sql = (String) metaObject.getValue(PluginUtils.DELEGATE_BOUNDSQL_SQL);
                for (String tableName : tenantTable) {
                    String sqlLowercase = sql.toLowerCase();
                    if (sqlLowercase.contains(tableName.toLowerCase())) {
                        for (String key : ignoreSqls) {
                            if (ms.getId().equals(key)) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
                // --------------------------- 以下为自定义过滤器常用的方法 ---------------------------
                // 获取这个 sql 语句的详细信息
                // MappedStatement ms = SqlParserHelper.getMappedStatement(metaObject);

                // 根据 sql 类型跳过多租户条件
                // if(ms.getSqlCommandType()== SqlCommandType.UPDATE){
                //     return true;
                // }

                // 根据 mapper 方法跳过多租户
                // if ("mapper路径.方法名".equals(ms.getId())) {
                //     return true;
                // }
                return true;
            }
        });
    }
//    /**
//     * mybatis-plus SQL执行效率插件【生产环境可以关闭】
//     */
//    @Bean
//    public PerformanceInterceptor performanceInterceptor() {
//        return new PerformanceInterceptor();
//    }

}