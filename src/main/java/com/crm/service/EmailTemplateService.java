package com.crm.service;

import com.crm.i18n.SupportedLocale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailTemplateService {

    private static final String TEMPLATE_DIR = "i18n/email/";

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public String renderHtml(String templateName, Locale locale, Map<String, String> variables) {
        String template = loadTemplate(templateName + ".html");
        String rendered = applyLocaleAttributes(template, locale);
        rendered = substituteVariables(rendered, variables);
        return rendered;
    }

    public String renderPlainText(String templateName, Locale locale, Map<String, String> variables) {
        String template = loadTemplate(templateName + ".txt");
        String rendered = substituteVariables(template, variables);
        return rendered.trim();
    }

    private String applyLocaleAttributes(String template, Locale locale) {
        String dir = SupportedLocale.isRtl(locale) ? "rtl" : "ltr";
        String lang = SupportedLocale.toCode(locale);
        return template
                .replace("{{dir}}", dir)
                .replace("{{lang}}", lang);
    }

    private String substituteVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String loadTemplate(String fileName) {
        return templateCache.computeIfAbsent(fileName, name -> {
            try {
                ClassPathResource resource = new ClassPathResource(TEMPLATE_DIR + name);
                return resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load email template: " + name, e);
            }
        });
    }
}
