package org.korhan.konithor;

import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClients
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.util.concurrent.Executor

@SpringBootApplication
@EnableScheduling
@EnableAsync
open class Konithor {

    @Bean
    open fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurerAdapter() {
            override fun addCorsMappings(registry: CorsRegistry?) {
                registry!!.addMapping("/**").allowedMethods("GET", "POST", "OPTIONS", "DELETE")
            }
        }
    }

    @Bean
    open fun httpClient(): HttpClient {
        val timeout = 30000
        val requestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build()
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()
    }

    @Bean
    open fun executor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.initialize()
        return executor
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Konithor::class.java, *args)
}

