document.querySelectorAll("[data-category-dashboard]").forEach((dashboard) => {
    const toggles = dashboard.querySelectorAll(".category-toggle");
    const panels = dashboard.querySelectorAll(".merchant-panel");

    const activate = (button) => {
        const targetId = button.dataset.target;
        const panel = document.getElementById(targetId);
        if (!panel) {
            return;
        }

        toggles.forEach((toggle) => toggle.classList.remove("is-active"));
        panels.forEach((merchantPanel) => merchantPanel.classList.remove("is-open"));
        button.classList.add("is-active");
        panel.classList.add("is-open");
    };

    toggles.forEach((button, index) => {
        if (index === 0) {
            activate(button);
        }

        button.addEventListener("click", () => activate(button));
    });
});
