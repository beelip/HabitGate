package com.habitgate.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** カラーテーマのカタログと、選択・ダークモードの適用を担う。 */
public final class Themes {
    public static final String KEY_THEME_ID = "theme_id";
    public static final String KEY_DARK = "dark_mode";
    public static final String KEY_FOLLOW_SYSTEM_DARK = "follow_system_dark";

    private Themes() {}

    /** 1つのテーマが持つ実際の配色一式。 */
    public static class Palette {
        public final int bg, surface, border, text, muted, primary, primarySoft, onPrimary, good, danger, editBg, hint;
        public final boolean dark;

        public Palette(int bg, int surface, int border, int text, int muted, int primary, int primarySoft,
                       int onPrimary, int good, int danger, int editBg, int hint, boolean dark) {
            this.bg = bg;
            this.surface = surface;
            this.border = border;
            this.text = text;
            this.muted = muted;
            this.primary = primary;
            this.primarySoft = primarySoft;
            this.onPrimary = onPrimary;
            this.good = good;
            this.danger = danger;
            this.editBg = editBg;
            this.hint = hint;
            this.dark = dark;
        }
    }

    /** 一覧に並ぶ1テーマ。ライト/ダークどちらか一方だけが定義されている場合がある。 */
    public static class Theme {
        public final String id;
        public final String label;
        final Palette light;
        final Palette dark;

        public Theme(String id, String label, Palette light, Palette dark) {
            this.id = id;
            this.label = label;
            this.light = light;
            this.dark = dark;
        }

        public Palette palette(boolean wantDark) {
            if (wantDark) {
                return dark != null ? dark : deriveDark(light);
            }
            return light != null ? light : dark;
        }

        public Palette preview() {
            return light != null ? light : dark;
        }
    }

    private static Palette light(int primary, int primarySoft) {
        return new Palette(
                0xFFF3F4F6, 0xFFFFFFFF, 0xFFE5E7EB, 0xFF111827, 0xFF6B7280,
                primary, primarySoft, 0xFFFFFFFF,
                0xFF059669, 0xFFDC2626,
                0xFFF9FAFB, 0xFF9CA3AF, false);
    }

    private static Palette dark(int primary, int primarySoft, int onPrimary) {
        return new Palette(
                0xFF0F1117, 0xFF181B22, 0xFF2A2F3A, 0xFFE5E7EB, 0xFF9CA3AF,
                primary, primarySoft, onPrimary,
                0xFF34D399, 0xFFF87171,
                0xFF20242D, 0xFF6B7280, true);
    }

    private static final List<Theme> ALL = Collections.unmodifiableList(buildAll());

    private static List<Theme> buildAll() {
        List<Theme> list = new ArrayList<>();

        // グループA: ライト・アクセント系
        list.add(new Theme("indigo", "インディゴ（標準）", light(0xFF4F46E5, 0xFFEEF2FF), null));
        list.add(new Theme("sky", "スカイ", light(0xFF0284C7, 0xFFE0F2FE), null));
        list.add(new Theme("emerald", "エメラルド", light(0xFF059669, 0xFFD1FAE5), null));
        list.add(new Theme("rose", "ローズ", light(0xFFE11D48, 0xFFFFE4E6), null));
        list.add(new Theme("amber", "アンバー", light(0xFFD97706, 0xFFFEF3C7), null));
        list.add(new Theme("violet", "ヴァイオレット", light(0xFF7C3AED, 0xFFEDE9FE), null));
        list.add(new Theme("teal", "ティール", light(0xFF0D9488, 0xFFCCFBF1), null));
        list.add(new Theme("mono", "モノクローム", light(0xFF111827, 0xFFE5E7EB), null));

        // グループB: ライト・フルカスタム系
        list.add(new Theme("sakura", "桜", new Palette(
                0xFFFDF2F8, 0xFFFFFFFF, 0xFFF5D0E1, 0xFF3F1D2E, 0xFFA0788C,
                0xFFDB2777, 0xFFFCE7F3, 0xFFFFFFFF, 0xFF059669, 0xFFBE123C,
                0xFFFCF3F8, 0xFFC79FB4, false), null));
        list.add(new Theme("matcha", "抹茶", new Palette(
                0xFFF4F7EE, 0xFFFDFEFA, 0xFFDDE6CC, 0xFF2C3323, 0xFF7A8568,
                0xFF4D7C0F, 0xFFECFCCB, 0xFFFFFFFF, 0xFF15803D, 0xFFB91C1C,
                0xFFF8FBF2, 0xFFA3B18A, false), null));
        list.add(new Theme("coffee", "珈琲", new Palette(
                0xFFF3EDE3, 0xFFFCF8F1, 0xFFE2D6C2, 0xFF3E2F1E, 0xFF8A7A63,
                0xFF8B5E2F, 0xFFF0E4D0, 0xFFFFFFFF, 0xFF4D7C0F, 0xFFB91C1C,
                0xFFF8F2E8, 0xFFB5A488, false), null));
        list.add(new Theme("washi", "和紙", new Palette(
                0xFFF7F4ED, 0xFFFFFDF7, 0xFFE6E0D0, 0xFF44403C, 0xFF8C8577,
                0xFFA16207, 0xFFFEF3C7, 0xFFFFFFFF, 0xFF4D7C0F, 0xFFB91C1C,
                0xFFFBF8F1, 0xFFB8B0A0, false), null));
        list.add(new Theme("newspaper", "新聞", new Palette(
                0xFFEFEDE6, 0xFFF8F7F2, 0xFFD6D3C4, 0xFF1F1E1B, 0xFF6E6B60,
                0xFF374151, 0xFFE2E4E9, 0xFFFFFFFF, 0xFF3F6212, 0xFF991B1B,
                0xFFF4F3EC, 0xFFA29F92, false), null));
        list.add(new Theme("ink", "インク", new Palette(
                0xFFFBFAF7, 0xFFFFFFFF, 0xFFE7E5DE, 0xFF1C1917, 0xFF78716C,
                0xFF0C4A6E, 0xFFE0F2FE, 0xFFFFFFFF, 0xFF15803D, 0xFFB91C1C,
                0xFFF7F6F2, 0xFFA8A29E, false), null));
        list.add(new Theme("mint", "ミント", new Palette(
                0xFFF0FDFA, 0xFFFFFFFF, 0xFFC7EDE4, 0xFF134E4A, 0xFF5F8A83,
                0xFF0D9488, 0xFFCCFBF1, 0xFFFFFFFF, 0xFF059669, 0xFFDC2626,
                0xFFF4FDFB, 0xFF99C7BF, false), null));
        list.add(new Theme("lavender", "ラベンダー", new Palette(
                0xFFF5F3FF, 0xFFFFFFFF, 0xFFE4DEFC, 0xFF372F63, 0xFF8079A8,
                0xFF6D28D9, 0xFFEDE9FE, 0xFFFFFFFF, 0xFF059669, 0xFFDC2626,
                0xFFF8F6FE, 0xFFB3ABD9, false), null));
        list.add(new Theme("ocean", "オーシャン", new Palette(
                0xFFECFEFF, 0xFFFFFFFF, 0xFFC5EDF2, 0xFF164E63, 0xFF5E8B99,
                0xFF0E7490, 0xFFCFFAFE, 0xFFFFFFFF, 0xFF059669, 0xFFDC2626,
                0xFFF2FDFE, 0xFF92C4CF, false), null));
        list.add(new Theme("forest", "フォレスト", new Palette(
                0xFFF0F7F1, 0xFFFCFEFC, 0xFFD3E5D6, 0xFF1C3024, 0xFF6B8574,
                0xFF166534, 0xFFDCFCE7, 0xFFFFFFFF, 0xFF15803D, 0xFFB91C1C,
                0xFFF5FAF6, 0xFF9DB8A5, false), null));
        list.add(new Theme("sunset", "サンセット", new Palette(
                0xFFFFF7ED, 0xFFFFFDF9, 0xFFFED7AA, 0xFF431407, 0xFFA16A47,
                0xFFEA580C, 0xFFFFEDD5, 0xFFFFFFFF, 0xFF16A34A, 0xFFDC2626,
                0xFFFFF9F0, 0xFFD3A57F, false), null));
        list.add(new Theme("bubblegum", "バブルガム", new Palette(
                0xFFFDF4FF, 0xFFFFFFFF, 0xFFF5D0FE, 0xFF4A044E, 0xFF9C6BA8,
                0xFFC026D3, 0xFFFAE8FF, 0xFFFFFFFF, 0xFF059669, 0xFFE11D48,
                0xFFFCF7FE, 0xFFD3A9DC, false), null));
        list.add(new Theme("lemon", "レモンソーダ", new Palette(
                0xFFFEFCE8, 0xFFFFFFFF, 0xFFF3E8A6, 0xFF422006, 0xFF8F8253,
                0xFFCA8A04, 0xFFFEF08A, 0xFFFFFFFF, 0xFF65A30D, 0xFFDC2626,
                0xFFFEFDF0, 0xFFC9BC85, false), null));
        list.add(new Theme("highcontrast", "ハイコントラスト", new Palette(
                0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFF333333,
                0xFF0000CC, 0xFFDDDDFF, 0xFFFFFFFF, 0xFF006600, 0xFFCC0000,
                0xFFFFFFFF, 0xFF555555, false), null));

        // グループC: ダーク・アクセント系
        list.add(new Theme("indigo_dark", "インディゴ・ナイト", null, dark(0xFF818CF8, 0xFF312E81, 0xFF1E1B4B)));
        list.add(new Theme("graphite", "グラファイト", null, dark(0xFFD1D5DB, 0xFF374151, 0xFF111827)));

        // グループD: ダーク・フルカスタム系
        list.add(new Theme("midnight", "ミッドナイトブルー", null, new Palette(
                0xFF0B1526, 0xFF12203A, 0xFF1E3A5F, 0xFFDBEAFE, 0xFF93A8C9,
                0xFF60A5FA, 0xFF1E3A8A, 0xFF172554, 0xFF34D399, 0xFFF87171,
                0xFF162844, 0xFF64789B, true)));
        list.add(new Theme("twilight", "トワイライト", null, new Palette(
                0xFF1E1B31, 0xFF2A2646, 0xFF413A6B, 0xFFEDEBFF, 0xFFA8A2CF,
                0xFFC4B5FD, 0xFF3730A3, 0xFF1E1B4B, 0xFF34D399, 0xFFFB7185,
                0xFF322D52, 0xFF7A73A6, true)));
        list.add(new Theme("neon_city", "ネオンシティ", null, new Palette(
                0xFF0A0A14, 0xFF14142B, 0xFF2E2E5E, 0xFFE0E7FF, 0xFF8B8BC0,
                0xFFE879F9, 0xFF4A044E, 0xFF4A044E, 0xFF22D3EE, 0xFFFB7185,
                0xFF1B1B38, 0xFF5F5F94, true)));
        list.add(new Theme("cyber_cyan", "真夜中のサイバー", null, new Palette(
                0xFF030712, 0xFF0F172A, 0xFF1E293B, 0xFFE2E8F0, 0xFF7C8DA6,
                0xFF22D3EE, 0xFF164E63, 0xFF083344, 0xFF34D399, 0xFFFB7185,
                0xFF131F35, 0xFF526381, true)));
        list.add(new Theme("terminal", "ターミナル", null, new Palette(
                0xFF041008, 0xFF0A1F14, 0xFF14532D, 0xFF86EFAC, 0xFF56A377,
                0xFF22C55E, 0xFF052E16, 0xFF052E16, 0xFF4ADE80, 0xFFF87171,
                0xFF0D2818, 0xFF3D7A56, true)));
        list.add(new Theme("retro_game", "レトロゲーム", null, new Palette(
                0xFF1A1A2E, 0xFF232345, 0xFF3D3D6B, 0xFFF1F1F1, 0xFFA0A0C8,
                0xFFFACC15, 0xFF713F12, 0xFF422006, 0xFF4ADE80, 0xFFF87171,
                0xFF2A2A50, 0xFF6E6E9E, true)));

        return list;
    }

    public static List<Theme> all() {
        return ALL;
    }

    public static Theme current(Context context) {
        String id = ReminderScheduler.prefs(context).getString(KEY_THEME_ID, ALL.get(0).id);
        for (Theme t : ALL) {
            if (t.id.equals(id)) return t;
        }
        return ALL.get(0);
    }

    public static boolean resolveDark(Context context) {
        SharedPreferences prefs = ReminderScheduler.prefs(context);
        if (prefs.getBoolean(KEY_FOLLOW_SYSTEM_DARK, false)) {
            int mask = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return mask == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        return prefs.getBoolean(KEY_DARK, false);
    }

    public static void apply(Context context) {
        Ui.applyTheme(current(context).palette(resolveDark(context)));
    }

    public static String signature(Context context) {
        return current(context).id + "|" + resolveDark(context);
    }

    public static void saveThemeId(Context context, String id) {
        ReminderScheduler.prefs(context).edit().putString(KEY_THEME_ID, id).apply();
    }

    public static void saveDark(Context context, boolean dark) {
        ReminderScheduler.prefs(context).edit().putBoolean(KEY_DARK, dark).apply();
    }

    public static void saveFollowSystemDark(Context context, boolean follow) {
        ReminderScheduler.prefs(context).edit().putBoolean(KEY_FOLLOW_SYSTEM_DARK, follow).apply();
    }

    static Palette deriveDark(Palette light) {
        int primary = lighten(light.primary, 0.35f);
        return new Palette(
                0xFF0F1117, 0xFF181B22, 0xFF2A2F3A, 0xFFE5E7EB, 0xFF9CA3AF,
                primary, blend(primary, 0xFF111827, 0.75f), 0xFF111827,
                lighten(light.good, 0.30f), lighten(light.danger, 0.30f),
                0xFF20242D, 0xFF6B7280, true);
    }

    static int lighten(int color, float f) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.round(r + (255 - r) * f);
        g = Math.round(g + (255 - g) * f);
        b = Math.round(b + (255 - b) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int blend(int color, int base, float baseRatio) {
        int r1 = (color >> 16) & 0xFF;
        int g1 = (color >> 8) & 0xFF;
        int b1 = color & 0xFF;
        int r2 = (base >> 16) & 0xFF;
        int g2 = (base >> 8) & 0xFF;
        int b2 = base & 0xFF;
        int r = Math.round(r1 * (1 - baseRatio) + r2 * baseRatio);
        int g = Math.round(g1 * (1 - baseRatio) + g2 * baseRatio);
        int b = Math.round(b1 * (1 - baseRatio) + b2 * baseRatio);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
