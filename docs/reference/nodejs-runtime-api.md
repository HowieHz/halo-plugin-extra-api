# Node Runtime API

`nodejs-runtime-api` 是给其他 Halo 插件使用的编译期依赖。

## Maven 坐标

```text
top.howiehz.halo.plugin.node.runtime:nodejs-runtime-api:<version>
```

当前版本跟随 `gradle.properties` 中的 `nodejsRuntimeVersion`，从 `1.0.0` 开始。

GitHub Packages 仓库：

```text
https://maven.pkg.github.com/HowieHz/halo-plugins
```

## Gradle 示例

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/HowieHz/halo-plugins")
        credentials {
            username = findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly "top.howiehz.halo.plugin.node.runtime:nodejs-runtime-api:<version>"
}
```

GitHub Packages 读取私有或受限包时需要 GitHub token。公开包在部分环境下也建议显式配置凭据，以避免 Maven 客户端鉴权差异。

## API 面

主要类型：

- `NodeRuntime`
- `NodeRuntimeMaintenance`
- `NodeModuleProvider`
- `NodeModuleDescriptor`
- `NodeModuleRef`
- `NodeCall`
- `NodeRuntimeStats`
- `NodeRuntimeRebuildResult`

调用方只依赖这些接口和 DTO，不直接依赖 Javet。
