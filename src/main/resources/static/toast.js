window.Toast = (() => {
    let host;

    function ensureHost() {
        if (host) {
            return host;
        }
        host = document.createElement("div");
        host.id = "toast-host";
        Object.assign(host.style, {
            position: "fixed",
            top: "20px",
            right: "20px",
            zIndex: "9999",
            display: "grid",
            gap: "10px",
            maxWidth: "320px"
        });
        document.body.appendChild(host);
        return host;
    }

    function show(message, type = "info") {
        const root = ensureHost();
        const toast = document.createElement("div");
        const palette = {
            info: { bg: "rgba(36, 24, 14, 0.92)", fg: "#fff4dd", border: "rgba(255,255,255,0.12)" },
            success: { bg: "rgba(30, 94, 49, 0.94)", fg: "#ecfff0", border: "rgba(255,255,255,0.12)" },
            error: { bg: "rgba(131, 36, 28, 0.95)", fg: "#fff0ec", border: "rgba(255,255,255,0.12)" }
        }[type] || { bg: "rgba(36, 24, 14, 0.92)", fg: "#fff4dd", border: "rgba(255,255,255,0.12)" };

        toast.textContent = message;
        Object.assign(toast.style, {
            padding: "12px 14px",
            borderRadius: "16px",
            background: palette.bg,
            color: palette.fg,
            border: `1px solid ${palette.border}`,
            boxShadow: "0 20px 40px rgba(0,0,0,0.16)",
            lineHeight: "1.5",
            fontSize: "14px",
            opacity: "0",
            transform: "translateY(-8px)",
            transition: "opacity 140ms ease, transform 140ms ease"
        });

        root.appendChild(toast);
        requestAnimationFrame(() => {
            toast.style.opacity = "1";
            toast.style.transform = "translateY(0)";
        });

        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transform = "translateY(-8px)";
            setTimeout(() => toast.remove(), 160);
        }, 2600);
    }

    return { show };
})();
