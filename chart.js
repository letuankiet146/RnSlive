let canvas, ctx;

function initChart() {
    canvas = document.getElementById('chart');
    ctx = canvas.getContext('2d');
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);
}

function resizeCanvas() {
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
    drawChart();
}

function drawChart() {
    if (!ctx) return;
    
    const width = canvas.width;
    const height = canvas.height;
    const padding = 60;
    
    // Clear canvas
    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, width, height);
    
    if (state.candlesticks.length === 0) return;
    
    // Calculate price range
    const allPrices = state.candlesticks.flatMap(c => [c.high, c.low]);
    if (state.currentResistance) allPrices.push(state.currentResistance);
    if (state.currentSupport) allPrices.push(state.currentSupport);
    
    const minPrice = Math.min(...allPrices) * 0.98;
    const maxPrice = Math.max(...allPrices) * 1.02;
    const priceRange = maxPrice - minPrice;
    
    // Draw resistance line
    if (state.currentResistance) {
        const y = height - padding - ((state.currentResistance - minPrice) / priceRange) * (height - 2 * padding);
        ctx.strokeStyle = '#ef5350';
        ctx.lineWidth = 2;
        ctx.setLineDash([5, 5]);
        ctx.beginPath();
        ctx.moveTo(padding, y);
        ctx.lineTo(width - padding, y);
        ctx.stroke();
        
        ctx.fillStyle = '#ef5350';
        ctx.font = '12px sans-serif';
        ctx.fillText(`R: $${state.currentResistance}`, width - padding + 10, y + 4);
    }
    
    // Draw support line
    if (state.currentSupport) {
        const y = height - padding - ((state.currentSupport - minPrice) / priceRange) * (height - 2 * padding);
        ctx.strokeStyle = '#26a69a';
        ctx.lineWidth = 2;
        ctx.setLineDash([5, 5]);
        ctx.beginPath();
        ctx.moveTo(padding, y);
        ctx.lineTo(width - padding, y);
        ctx.stroke();
        
        ctx.fillStyle = '#26a69a';
        ctx.font = '12px sans-serif';
        ctx.fillText(`S: $${state.currentSupport}`, width - padding + 10, y + 4);
    }
    
    // Draw candlesticks
    ctx.setLineDash([]);
    const candleWidth = (width - 2 * padding) / (state.candlesticks.length * 1.5);
    const spacing = candleWidth * 0.5;
    
    state.candlesticks.forEach((candle, i) => {
        const x = padding + i * (candleWidth + spacing) + candleWidth / 2;
        
        const openY = height - padding - ((candle.open - minPrice) / priceRange) * (height - 2 * padding);
        const closeY = height - padding - ((candle.close - minPrice) / priceRange) * (height - 2 * padding);
        const highY = height - padding - ((candle.high - minPrice) / priceRange) * (height - 2 * padding);
        const lowY = height - padding - ((candle.low - minPrice) / priceRange) * (height - 2 * padding);
        
        const isGreen = candle.close >= candle.open;
        const color = isGreen ? '#26a69a' : '#ef5350';
        
        // Draw wick
        ctx.strokeStyle = color;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(x, highY);
        ctx.lineTo(x, lowY);
        ctx.stroke();
        
        // Draw body
        ctx.fillStyle = color;
        const bodyTop = Math.min(openY, closeY);
        const bodyHeight = Math.abs(closeY - openY) || 1;
        ctx.fillRect(x - candleWidth / 2, bodyTop, candleWidth, bodyHeight);
    });
    
    // Draw axes
    ctx.strokeStyle = '#2a2a3e';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padding, padding);
    ctx.lineTo(padding, height - padding);
    ctx.lineTo(width - padding, height - padding);
    ctx.stroke();
    
    // Draw price labels
    ctx.fillStyle = '#a0a0b0';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'right';
    
    for (let i = 0; i <= 5; i++) {
        const price = minPrice + (priceRange * i / 5);
        const y = height - padding - (i / 5) * (height - 2 * padding);
        ctx.fillText(`$${price.toFixed(2)}`, padding - 10, y + 4);
    }
}
