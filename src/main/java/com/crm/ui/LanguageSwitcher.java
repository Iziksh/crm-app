package com.crm.ui;

import com.crm.i18n.SupportedLocale;
import com.crm.service.LocaleService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;

public class LanguageSwitcher extends Select<String> {

    public LanguageSwitcher(LocaleService localeService, TranslationService i18n, boolean compact) {
        if (compact) {
            setAriaLabel(i18n.translate("language.label"));
        } else {
            setLabel(i18n.translate("language.label"));
        }
        setItems(SupportedLocale.ALL.stream().map(SupportedLocale::toCode).toList());
        setItemLabelGenerator(code -> switch (code) {
            case "en" -> i18n.translate("language.en");
            case "he" -> i18n.translate("language.he");
            default -> code;
        });
        setValue(SupportedLocale.toCode(localeService.getCurrentLocale()));
        setWidth("7.5rem");
        addThemeVariants(SelectVariant.LUMO_SMALL);
        addValueChangeListener(e -> {
            if (!e.isFromClient() || e.getValue() == null) {
                return;
            }
            SupportedLocale.fromCode(e.getValue()).ifPresent(locale ->
                    getUI().ifPresent(ui -> localeService.switchLocale(locale, ui)));
        });
    }
}
