let count = 0;
const counterEl = document.getElementById('counter-val');
const logEl = document.getElementById('log-output');

function updateDisplay() {
    counterEl.textContent = count;
    logEl.textContent = `Count updated: ${count} at ${new Date().toLocaleTimeString()}`;
    console.log(`[CodeForge Preview] Counter changed to: ${count}`);
}

document.getElementById('btn-inc').addEventListener('click', () => {
    count++;
    updateDisplay();
});

document.getElementById('btn-dec').addEventListener('click', () => {
    count--;
    updateDisplay();
});

console.log("[CodeForge Preview] Web Starter initialized successfully!");
