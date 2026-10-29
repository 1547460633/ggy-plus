# MyBatis / MyBatis-Plus 拦截器使用说明

本文记录 ggy-plus 当前 MyBatis、MyBatis-Plus 插件的实际配置、注册方式及扩展规范。

## 1. 先区分两套插件接口

`SqlSessionFactory` 的最终插件链属于 **MyBatis**。MyBatis-Plus 是在这条链上增加了一层组合插件。

```text
SqlSessionFactory.plugins
├─ MyBatis 原生 Interceptor
│  ├─ MybatisEncryptInterceptor       （项目自定义）
│  └─ MybatisDecryptInterceptor       （项目自定义）
└─ MybatisPlusInterceptor             （MyBatis-Plus 提供，自己也是原生 Interceptor）
   ├─ PlusDataPermissionInterceptor   （项目自定义 InnerInterceptor）
   ├─ PaginationInnerInterceptor      （MyBatis-Plus 内置）
   └─ OptimisticLockerInnerInterceptor（MyBatis-Plus 内置）
```

| 接口/类 | 归属 | 是否可直接注册到 `SqlSessionFactory` |
| --- | --- | --- |
| `org.apache.ibatis.plugin.Interceptor` | MyBatis | 可以 |
| `com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor` | MyBatis-Plus | 可以；它本身实现了 MyBatis 的 `Interceptor` |
| `com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor` | MyBatis-Plus | 不可以；必须先通过 `MybatisPlusInterceptor#addInnerInterceptor` 加入 |

Bean 的**名称不参与插件识别**。例如 `mybatisEncryptInterceptor` 只是 `@Bean` 方法的默认 Bean 名；真正重要的是它实现了 `Interceptor` 接口。

## 2. 当前已启用的 MyBatis-Plus 插件

配置入口：[common/common-mybatis/src/main/java/org/ggy/common/mybatis/config/MybatisPlusConfig.java](common/common-mybatis/src/main/java/org/ggy/common/mybatis/config/MybatisPlusConfig.java)。

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(dataPermissionInterceptor());
    interceptor.addInnerInterceptor(paginationInnerInterceptor());
    interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
    return interceptor;
}
```

### 2.1 数据权限：`PlusDataPermissionInterceptor`

- 类型：项目自定义 `InnerInterceptor`。
- 代码位置：[PlusDataPermissionInterceptor.java](common/common-mybatis/src/main/java/org/ggy/common/mybatis/interceptor/PlusDataPermissionInterceptor.java)。
- 作用：在 `SELECT`、`UPDATE`、`DELETE` 的 SQL 执行前，按当前用户的数据权限向 `WHERE` 条件追加过滤表达式。
- 忽略：代码通过 `InterceptorIgnoreHelper.willIgnoreDataPermission(...)` 支持 MyBatis-Plus 的 `@InterceptorIgnore` 机制。
- 适用场景：部门、角色、本人等行级数据权限控制。

### 2.2 分页：`PaginationInnerInterceptor`

- 类型：MyBatis-Plus 内置插件。
- 当前配置：自动识别数据库类型，`overflow=true`；页码超过总页数时按插件规则处理。
- 作用：配合 MyBatis-Plus 的分页参数生成分页 SQL 和总数查询。
- 适用场景：列表接口的分页查询。

### 2.3 乐观锁：`OptimisticLockerInnerInterceptor`

- 类型：MyBatis-Plus 内置插件。
- 作用：对标注 `@Version` 的实体字段生效，更新时将版本号写入 `WHERE` 条件，避免并发更新互相覆盖。
- 适用场景：订单、库存、配置等“读取后编辑再保存”的并发写入。
- 注意：实体没有 `@Version` 字段时，该插件不会自动给所有表加锁。

### 2.4 插件顺序

当前顺序为：**数据权限 → 分页 → 乐观锁**。SQL 改写型插件的顺序会影响最终 SQL，不应随意调整。新增的租户、动态表名等 SQL 改写插件，一般应放在分页前。

## 3. 当前存在但默认关闭的字段加解密拦截器

代码位于 `common-encrypt`：

- [MybatisEncryptInterceptor.java](common/common-encrypt/src/main/java/org/ggy/common/encrypt/interceptor/MybatisEncryptInterceptor.java)：`INSERT` / `UPDATE` 绑定参数前加密。
- [MybatisDecryptInterceptor.java](common/common-encrypt/src/main/java/org/ggy/common/encrypt/interceptor/MybatisDecryptInterceptor.java)：查询结果映射后解密。
- [EncryptorAutoConfiguration.java](common/common-encrypt/src/main/java/org/ggy/common/encrypt/config/EncryptorAutoConfiguration.java)：以 `@Bean` 注册这两个原生 MyBatis `Interceptor`。

只有以下配置为 `true` 时，自动配置才会注册上述 Bean：

```yaml
mybatis-encryptor:
  enable: true
```

当前 [admin/src/main/resources/application.yml](admin/src/main/resources/application.yml) 为 `false`，所以加解密插件未生效。

开启后也**不会加密所有字段**，仅处理实体中带有 `@EncryptField` 的字段：

```java
@EncryptField(algorithm = AlgorithmType.AES, password = "示例密钥")
private String idCard;
```

生产环境不要把 AES/SM4 密钥或 RSA/SM2 私钥硬编码到注解或 Git 仓库，应从环境变量、密钥服务或受保护的配置中心读取。`BASE64` 仅是编码，不是保密加密。

## 4. 如何新增插件

### 4.1 新增 MyBatis-Plus `InnerInterceptor`

适用于分页、租户、动态表名、SQL 防护等 MyBatis-Plus 扩展点。

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(/* handler */));
    interceptor.addInnerInterceptor(dataPermissionInterceptor());
    interceptor.addInnerInterceptor(paginationInnerInterceptor());
    interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
    return interceptor;
}
```

不要把 `InnerInterceptor` 单独定义为普通 MyBatis 插件；它需要由 `MybatisPlusInterceptor` 包装后才会进入 `SqlSessionFactory` 插件链。

### 4.2 新增原生 MyBatis `Interceptor`

适用于需要拦截 MyBatis 的 `Executor`、`StatementHandler`、`ParameterHandler`、`ResultSetHandler` 的场景，例如字段加密、SQL 审计、参数/结果转换。

```java
@Intercepts({
    @Signature(type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class})
})
public class CustomMybatisInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        return invocation.proceed();
    }
}

@Configuration
public class CustomMybatisConfiguration {

    @Bean
    public CustomMybatisInterceptor customMybatisInterceptor() {
        return new CustomMybatisInterceptor();
    }
}
```

在 Spring Boot + MyBatis-Plus Starter 中，实现 `org.apache.ibatis.plugin.Interceptor` 并注册为 Bean 后，自动配置会收集它并放入 `SqlSessionFactory` 的插件链。仅在 Service 中 `@Autowired` 一个拦截器对象，不会使其参与 SQL 执行。

## 5. 其他可选的 MyBatis-Plus 内置插件

下列插件在当前项目中**没有注册**；需要时评估后再加入 `mybatisPlusInterceptor()`：

| 插件 | 常见用途 |
| --- | --- |
| `TenantLineInnerInterceptor` | 多租户 SQL 条件注入 |
| `DynamicTableNameInnerInterceptor` | 按规则动态改写表名，如按月分表 |
| `BlockAttackInnerInterceptor` | 阻止无 `WHERE` 条件的 `UPDATE` / `DELETE` |
| `IllegalSQLInnerInterceptor` | 拦截不符合规则或风险较高的 SQL |

启用 SQL 防护插件前，应针对现有 Mapper、批处理任务和管理脚本做回归测试，避免误拦截合法操作。

## 6. `TypeHandler` 与拦截器的差别

`TypeHandler` 是 MyBatis 原生的“单个字段类型转换”机制，不是 MyBatis-Plus 独有：

```text
TypeHandler：Java 字段值 <-> JDBC / 数据库列值
Interceptor：一次 Mapper / SQL 执行前后，可处理 SQL、参数或结果对象
```

JSON、枚举、日期类型转换适合优先使用 `TypeHandler`；当前项目的字段加密需要扫描 `@EncryptField` 并同时处理写入与查询结果，故采用原生 MyBatis `Interceptor`。

## 7. 多数据源注意事项

如果使用动态数据源、但所有数据源共享**一个** `SqlSessionFactory`，插件链也是共享的：注册一次即作用于所有路由到的物理数据源。若要仅对某个数据源生效，应在插件内部按当前数据源、Mapper 或表名判断。

如果每个数据源各自创建**独立的** `SqlSessionFactory`，每个 Factory 都有独立插件链；需要差异化插件时，应在对应 Factory 的 `setPlugins(...)` 中显式配置。

## 8. 非拦截器但相关的当前配置

以下 Bean 与 MyBatis-Plus 功能相关，但不属于插件链：

| Bean/机制 | 代码位置 | 作用 |
| --- | --- | --- |
| `MetaObjectHandler` | `MybatisPlusConfig#metaObjectHandler` | 新增、更新时自动填充字段 |
| `IdentifierGenerator` | `MybatisPlusConfig#idGenerator` | 使用网卡信息生成雪花 ID，降低集群重复风险 |
| `PostInitTableInfoHandler` | `MybatisPlusConfig#postInitTableInfoHandler` | 初始化实体表信息后的处理 |
| `MybatisExceptionHandler` | `MybatisPlusConfig#mybatisExceptionHandler` | 数据库异常处理 |

