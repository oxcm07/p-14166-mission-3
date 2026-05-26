(function() {
    const STORAGE_KEY = "theme";
    const THEME_VALUES = ["auto", "light", "dark"];
    const labels = {
        auto: "자동",
        light: "밝게",
        dark: "어둡게"
    };
    const mediaQuery = window.matchMedia ? window.matchMedia("(prefers-color-scheme: dark)") : null;

    function getStoredPreference() {
        try {
            const value = localStorage.getItem(STORAGE_KEY);
            return THEME_VALUES.includes(value) ? value : "auto";
        } catch (e) {
            return "auto";
        }
    }

    function storePreference(preference) {
        try {
            localStorage.setItem(STORAGE_KEY, preference);
        } catch (e) {
            // Storage can be unavailable in private browsing or restricted environments.
        }
    }

    function resolveTheme(preference) {
        if (preference === "dark" || preference === "light") {
            return preference;
        }
        return mediaQuery && mediaQuery.matches ? "dark" : "light";
    }

    function updateControls(preference) {
        const toggle = document.getElementById("themeToggle");
        const autoIcon = document.getElementById("themeToggleAuto");
        const sunIcon = document.getElementById("themeToggleSun");
        const moonIcon = document.getElementById("themeToggleMoon");
        const label = labels[preference] || labels.auto;

        if (toggle) {
            toggle.setAttribute("aria-label", "테마 " + label);
            toggle.setAttribute("title", "테마 " + label);
            toggle.dataset.themeValue = preference;
        }

        if (autoIcon && sunIcon && moonIcon) {
            autoIcon.classList.toggle("d-none", preference !== "auto");
            sunIcon.classList.toggle("d-none", preference !== "light");
            moonIcon.classList.toggle("d-none", preference !== "dark");
        }
    }

    function nextPreference(preference) {
        const index = THEME_VALUES.indexOf(preference);
        return THEME_VALUES[(index + 1) % THEME_VALUES.length];
    }

    function applyTheme(preference, persist) {
        const safePreference = THEME_VALUES.includes(preference) ? preference : "auto";
        const resolvedTheme = resolveTheme(safePreference);

        if (persist) {
            storePreference(safePreference);
        }

        document.documentElement.setAttribute("data-bs-theme", resolvedTheme);
        document.documentElement.setAttribute("data-theme-preference", safePreference);
        updateControls(safePreference);
        document.dispatchEvent(new CustomEvent("sbb:themechange", {
            detail: {
                preference: safePreference,
                theme: resolvedTheme
            }
        }));
    }

    document.addEventListener("DOMContentLoaded", function() {
        applyTheme(getStoredPreference(), false);

        const toggle = document.getElementById("themeToggle");
        if (toggle) {
            toggle.addEventListener("click", function() {
                applyTheme(nextPreference(getStoredPreference()), true);
            });
        }
    });

    if (mediaQuery) {
        mediaQuery.addEventListener("change", function() {
            if (getStoredPreference() === "auto") {
                applyTheme("auto", false);
            }
        });
    }

    window.sbbTheme = {
        apply: function(preference) {
            applyTheme(preference, true);
        },
        current: function() {
            const preference = getStoredPreference();
            return {
                preference: preference,
                theme: resolveTheme(preference)
            };
        }
    };
})();
