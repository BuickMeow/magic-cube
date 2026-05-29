package com.yuwen.magiccube;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 数据库连接测试
 */
@Component
public class DatabaseTest implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseTest(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        System.out.println("========== 开始测试数据库连接 ==========");
        
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            // 测试查询
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            System.out.println("✓ 数据库连接成功! 测试结果：" + result);
            
            // 获取数据库名称
            String dbName = jdbcTemplate.queryForObject(
                "SELECT DATABASE()", String.class);
            System.out.println("✓ 当前数据库：" + dbName);
            
            // 列出所有表
            System.out.println("\n========== 数据库中的表 ==========");
            var tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
            if (tables.isEmpty()) {
                System.out.println("⚠ 警告：数据库中没有找到任何表!");
                System.out.println("  Hibernate 应该会自动创建表，请等待项目启动完成");
            } else {
                for (String table : tables) {
                    System.out.println("  - " + table);
                    
                    // 统计记录数
                    try {
                        Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM " + table, Integer.class);
                        System.out.println("    记录数：" + count);
                    } catch (Exception e) {
                        // 忽略统计失败的表
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("✗ 数据库连接失败!");
            System.err.println("错误信息：" + e.getMessage());
            System.err.println("\n可能的原因:");
            System.err.println("1. MySQL 服务未启动");
            System.err.println("2. 数据库 magic_cube 不存在");
            System.err.println("3. 用户名或密码错误");
            System.err.println("4. 端口不是 3306");
        }
        
        System.out.println("\n========== 数据库测试完成 ==========");
    }
}
