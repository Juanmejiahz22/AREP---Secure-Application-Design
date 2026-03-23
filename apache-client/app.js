const API_BASE_URL = "https://ec2-54-146-31-33.compute-1.amazonaws.com:8443";

const output = document.getElementById("output");
let authToken = "";

function print(data) {
    output.textContent = JSON.stringify(data, null, 2);
}

function normalizeUsername(value) {
    return (value || "").trim();
}

async function postJson(path, body) {
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
        mode: 'cors',
        credentials: 'omit'
    });

    const payload = await response.json();
    if (!response.ok) {
        throw new Error(payload.error || "Error en la solicitud");
    }

    return payload;
}

document.getElementById("registerBtn").addEventListener("click", async () => {
    try {
        const username = normalizeUsername(document.getElementById("registerUser").value);
        const password = document.getElementById("registerPass").value;

        if (!username || !password) {
            throw new Error("Usuario y contrasena son obligatorios");
        }

        const data = await postJson("/api/auth/register", { username, password });
        print(data);
    } catch (error) {
        print({ error: error.message });
    }
});

document.getElementById("loginBtn").addEventListener("click", async () => {
    try {
        const username = normalizeUsername(document.getElementById("loginUser").value);
        const password = document.getElementById("loginPass").value;

        if (!username || !password) {
            throw new Error("Usuario y contrasena son obligatorios");
        }

        const data = await postJson("/api/auth/login", { username, password });
        authToken = data.token || "";
        print(data);
    } catch (error) {
        print({ error: error.message });
    }
});

document.getElementById("pingBtn").addEventListener("click", async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/api/secure/ping`, {
            method: 'GET',
            mode: 'cors',
            credentials: 'omit'
        });

        const payload = await response.json();
        if (!response.ok) {
            throw new Error(payload.error || "Error en ping");
        }
        print(payload);
    } catch (error) {
        print({ error: error.message });
    }
});

document.getElementById("secureBtn").addEventListener("click", async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/api/secure/hello`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${authToken}`
            },
            mode: 'cors',
            credentials: 'omit'
        });

        const payload = await response.json();
        if (!response.ok) {
            throw new Error(payload.error || "No autorizado");
        }
        print(payload);
    } catch (error) {
        print({ error: error.message });
    }
});
