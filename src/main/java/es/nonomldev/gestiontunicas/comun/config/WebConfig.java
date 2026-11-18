package es.nonomldev.gestiontunicas.comun.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Las páginas se devuelven con un formato estable:
 * {@code { "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }}.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {
}
