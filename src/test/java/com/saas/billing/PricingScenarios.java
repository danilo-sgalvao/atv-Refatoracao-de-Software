package com.saas.billing;

import java.time.LocalDate;
import java.util.List;

/**
 * Cenários de caracterização do cálculo de preço do legado.
 * Datas fixas: JUNHO = 2026-06-15 (fora de novembro), NOVEMBRO = 2026-11-15.
 * monthsActive = (anos * 12) + diferença de meses (ignora o dia do mês).
 */
public final class PricingScenarios {

    static final LocalDate JUNHO = LocalDate.of(2026, 6, 15);
    static final LocalDate NOVEMBRO = LocalDate.of(2026, 11, 15);

    private PricingScenarios() {
    }

    public static List<PricingScenario> all() {
        return List.of(
                // --- desconto por tempo de casa (limites estritos: >12 e >24) ---
                new PricingScenario("5 meses: sem desconto", 100, LocalDate.of(2026, 1, 10), JUNHO, 0, null, false, 100.00),
                new PricingScenario("12 meses: limite, sem desconto", 100, LocalDate.of(2025, 6, 1), JUNHO, 0, null, false, 100.00),
                new PricingScenario("13 meses: 10%", 100, LocalDate.of(2025, 5, 1), JUNHO, 0, null, false, 90.00),
                new PricingScenario("24 meses: limite, ainda 10%", 100, LocalDate.of(2024, 6, 1), JUNHO, 0, null, false, 90.00),
                new PricingScenario("25 meses: 15%", 100, LocalDate.of(2024, 5, 1), JUNHO, 0, null, false, 85.00),

                // --- ONG ---
                new PricingScenario("ONG: 30% off", 100, LocalDate.of(2026, 1, 10), JUNHO, 0, null, true, 70.00),
                new PricingScenario("ONG com 13 meses: tempo de casa antes da ONG", 100, LocalDate.of(2025, 5, 1), JUNHO, 0, null, true, 63.00),

                // --- WELCOME10 (vale se monthsActive <= 1, case-insensitive) ---
                new PricingScenario("WELCOME10 com 1 mes: aplica", 100, LocalDate.of(2026, 5, 20), JUNHO, 0, "WELCOME10", false, 90.00),
                new PricingScenario("WELCOME10 com 2 meses: nao aplica", 100, LocalDate.of(2026, 4, 1), JUNHO, 0, "WELCOME10", false, 100.00),
                new PricingScenario("welcome10 minusculo com 0 meses: aplica", 100, LocalDate.of(2026, 6, 1), JUNHO, 0, "welcome10", false, 90.00),

                // --- BLACKFRIDAY (so em novembro, desconto fixo de 30) ---
                new PricingScenario("BLACKFRIDAY fora de novembro: nao aplica", 100, LocalDate.of(2026, 1, 10), JUNHO, 0, "BLACKFRIDAY", false, 100.00),
                new PricingScenario("BLACKFRIDAY em novembro: -30", 100, LocalDate.of(2026, 1, 10), NOVEMBRO, 0, "BLACKFRIDAY", false, 70.00),
                new PricingScenario("BLACKFRIDAY maior que o preco: piso em 0", 20, LocalDate.of(2026, 1, 10), NOVEMBRO, 0, "BLACKFRIDAY", false, 0.00),

                // --- promo ausente ou invalida ---
                new PricingScenario("promo null", 100, LocalDate.of(2026, 1, 10), JUNHO, 0, null, false, 100.00),
                new PricingScenario("promo em branco", 100, LocalDate.of(2026, 1, 10), JUNHO, 0, "   ", false, 100.00),
                new PricingScenario("promo desconhecida", 100, LocalDate.of(2026, 1, 10), JUNHO, 0, "XYZ", false, 100.00),

                // --- atraso: multa 2% + juros 0,1% ao dia, sobre o preco ja descontado ---
                new PricingScenario("atraso de 10 dias", 100, LocalDate.of(2026, 1, 10), JUNHO, 10, null, false, 103.00),
                new PricingScenario("atraso de 1 dia", 100, LocalDate.of(2026, 1, 10), JUNHO, 1, null, false, 102.10),

                // --- combinacoes e ordem das regras ---
                new PricingScenario("25 meses + ONG + BLACKFRIDAY + 20 dias de atraso", 100, LocalDate.of(2024, 10, 1), NOVEMBRO, 20, "BLACKFRIDAY", true, 30.68),
                new PricingScenario("preco negativo + atraso: piso so no final", 20, LocalDate.of(2026, 1, 10), NOVEMBRO, 5, "BLACKFRIDAY", false, 0.00),

                // --- valores com centavos ---
                new PricingScenario("49,90 com 13 meses", 49.90, LocalDate.of(2025, 5, 1), JUNHO, 0, null, false, 44.91),
                new PricingScenario("49,90 ONG", 49.90, LocalDate.of(2026, 1, 10), JUNHO, 0, null, true, 34.93)
        );
    }
}
