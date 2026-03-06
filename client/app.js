function getRemainingTime(currentDateTime, interval, candleStartTime) {
    const now = new Date(currentDateTime);
    const timeframeMs = intervalToMilliseconds(interval);
    
    if (candleStartTime) {
        // Calculate based on actual candle start time
        const candleEndTime = candleStartTime + timeframeMs;
        const remainingMs = candleEndTime - now.getTime();
        
        if (remainingMs <= 0) {
            return { totalMilliseconds: 0, hours: 0, minutes: 0, seconds: 0 };
        }
        
        const seconds = Math.floor((remainingMs / 1000) % 60);
        const minutes = Math.floor((remainingMs / (1000 * 60)) % 60);
        const hours = Math.floor(remainingMs / (1000 * 60 * 60));
        const days = Math.floor(remainingMs / (1000 * 60 * 60 * 24));
        
        return { totalMilliseconds: remainingMs, days, hours, minutes, seconds };
    } else {
        // Fallback to modulo calculation
        const currentMs = now.getTime();
        const remainder = currentMs % timeframeMs;
        const remainingMs = timeframeMs - remainder;
        
        const seconds = Math.floor((remainingMs / 1000) % 60);
        const minutes = Math.floor((remainingMs / (1000 * 60)) % 60);
        const hours = Math.floor(remainingMs / (1000 * 60 * 60));
        const days = Math.floor(remainingMs / (1000 * 60 * 60 * 24));
        
        return { totalMilliseconds: remainingMs, days, hours, minutes, seconds };
    }
}

function intervalToMilliseconds(interval) {
    const value = parseInt(interval);
    const unit = interval.slice(-1); // Keep case-sensitive
    
    switch(unit) {
        case 'm': return value * 60 * 1000; // minutes
        case 'h': return value * 60 * 60 * 1000; // hours
        case 'H': return value * 60 * 60 * 1000; // hours
        case 'd': return value * 24 * 60 * 60 * 1000; // days
        case 'D': return value * 24 * 60 * 60 * 1000; // days
        case 'w': return value * 7 * 24 * 60 * 60 * 1000; // weeks
        case 'W': return value * 7 * 24 * 60 * 60 * 1000; // weeks
        case 'M': return value * 30 * 24 * 60 * 60 * 1000; // months (approximate)
        default: return 60 * 1000; // default 1 minute
    }
}

function intervalToMinutes(interval) {
    return intervalToMilliseconds(interval) / (60 * 1000);
}

console.log('Script loaded!');

// Load kLines data for specific interval
async function loadKLines(interval) {
    try {
        const response = await fetch(`http://localhost:8080/kLines?interval=${interval}`);
        const kLines = await response.json();
        
        console.log('Loaded kLines:', kLines.length, 'for interval:', interval);
        
        // Convert kLines to candle format
        state.allCandles = kLines.map(kline => ({
            time: Math.floor(kline.openTime / 1000),
            open: kline.openPrice,
            high: kline.highPrice,
            low: kline.lowPrice,
            close: kline.closePrice
        }));
        
        state.dataIndex = state.allCandles.length;
        
        // Draw initial chart
        redrawChart();
        
        console.log('Chart initialized with', state.allCandles.length, 'candles');
    } catch (error) {
        console.error('Error loading kLines:', error);
    }
}

function connectSSE() {
    const eventSource = new EventSource('http://localhost:8080/prices');

    eventSource.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            handlePriceUpdate(data);
        } catch (error) {
            console.error('Error parsing SSE data:', error);
        }
    };

    eventSource.onerror = (error) => {
        console.error('SSE connection error:', error);
        eventSource.close();
        setTimeout(() => {
            console.log('Attempting to reconnect...');
            connectSSE();
        }, 5000);
    };

    return eventSource;
}

function handlePriceUpdate(data) {
    if (!data || typeof data.middle === 'undefined') {
        console.error('Invalid price data received:', data);
        return;
    }

    const price = data.middle;
    const currentTime = Date.now();

    document.getElementById('current-price').textContent = `$${data.middle}`;
    document.getElementById('resistance-level').textContent = `$${data.top}`;
    document.getElementById('support-level').textContent = `$${data.bottom}`;
    document.getElementById('data-count').textContent = `${state.allCandles.length}`;

    if (resistanceLine) {
        candleSeries.removePriceLine(resistanceLine);
    }
    resistanceLine = candleSeries.createPriceLine({
        price: data.top,
        color: 'rgba(255, 107, 107, 0.5)',
        lineWidth: 2,
        lineStyle: LightweightCharts.LineStyle.Dashed,
        axisLabelVisible: true,
        title: 'R',
    });

    if (supportLine) {
        candleSeries.removePriceLine(supportLine);
    }
    supportLine = candleSeries.createPriceLine({
        price: data.bottom,
        color: 'rgba(81, 207, 102, 0.5)',
        lineWidth: 2,
        lineStyle: LightweightCharts.LineStyle.Dashed,
        axisLabelVisible: true,
        title: 'S',
    });

    // Update or create current candle
    if (state.allCandles.length > 0) {
        const lastCandle = state.allCandles[state.allCandles.length - 1];
        const timeframeMs = intervalToMilliseconds(state.currentInterval);
        const lastCandleEndTime = (lastCandle.time * 1000) + timeframeMs;
        
        // Check if we need a new candle or update the current one
        if (currentTime >= lastCandleEndTime) {
            // Create new candle - align to interval boundary
            const newCandleTime = Math.floor(currentTime / timeframeMs) * timeframeMs / 1000;
            const newCandle = {
                time: newCandleTime,
                open: price,
                high: price,
                low: price,
                close: price
            };
            state.allCandles.push(newCandle);
        } else {
            // Update current candle with live price
            lastCandle.close = price;
            lastCandle.high = Math.max(lastCandle.high, price);
            lastCandle.low = Math.min(lastCandle.low, price);
        }
    } else {
        // First candle - align to interval boundary
        const timeframeMs = intervalToMilliseconds(state.currentInterval);
        const newCandleTime = Math.floor(currentTime / timeframeMs) * timeframeMs / 1000;
        const newCandle = {
            time: newCandleTime,
            open: price,
            high: price,
            low: price,
            close: price
        };
        state.allCandles.push(newCandle);
    }

    redrawChart();
}

let chart = null;
let candleSeries = null;
let resistanceLine = null;
let supportLine = null;
let eventSource = null;

const state = {
    dataIndex: 0,
    allCandles: [],
    currentInterval: '1m',
    countdownId: null,
    startTime: Date.now()
};

function formatTime(timestamp) {
    const date = new Date(timestamp * 1000);
    const options = {
        weekday: 'short',
        day: '2-digit',
        month: 'short',
        year: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    };
    return date.toLocaleString('en-GB', options);
}

function formatCountdown(remaining) {
    // Format based on the size of the remaining time
    if (remaining.days > 0) {
        return `${remaining.days}d ${remaining.hours.toString().padStart(2, '0')}:${remaining.minutes.toString().padStart(2, '0')}:${remaining.seconds.toString().padStart(2, '0')}`;
    } else if (remaining.hours > 0) {
        return `${remaining.hours.toString().padStart(2, '0')}:${remaining.minutes.toString().padStart(2, '0')}:${remaining.seconds.toString().padStart(2, '0')}`;
    } else {
        return `${remaining.minutes.toString().padStart(2, '0')}:${remaining.seconds.toString().padStart(2, '0')}`;
    }
}

function updateCountdown() {
    const now = new Date();
    
    // Get the current candle start time
    let candleStartTimeMs = null;
    if (state.allCandles.length > 0) {
        const lastCandle = state.allCandles[state.allCandles.length - 1];
        candleStartTimeMs = lastCandle.time * 1000;
    }
    
    const remaining = getRemainingTime(now, state.currentInterval, candleStartTimeMs);
    const countdownText = formatCountdown(remaining);

    const countdownEl = document.getElementById('countdown-display');
    if (countdownEl) {
        countdownEl.textContent = countdownText;
    }
}

function updatePriceCountdown() {
    const countdownEl = document.getElementById('price-countdown');
    if (!countdownEl || state.allCandles.length === 0) {
        if (countdownEl) countdownEl.style.display = 'none';
        return;
    }

    const latestCandle = state.allCandles[state.allCandles.length - 1];
    const candleStartTimeMs = latestCandle.time * 1000;
    
    const remaining = getRemainingTime(new Date(), state.currentInterval, candleStartTimeMs);
    
    // Format countdown for price display (shorter format)
    let countdownText;
    if (remaining.days > 0) {
        countdownText = `${remaining.days}d ${remaining.hours}h`;
    } else if (remaining.hours > 0) {
        countdownText = `${remaining.hours}h ${remaining.minutes}m`;
    } else {
        countdownText = `${remaining.minutes.toString().padStart(2, '0')}:${remaining.seconds.toString().padStart(2, '0')}`;
    }

    const currentPrice = latestCandle.close;

    try {
        const coordinate = candleSeries.priceToCoordinate(currentPrice);

        if (coordinate !== null && coordinate !== undefined) {
            countdownEl.innerHTML = `<div style="font-size: 16px;">${currentPrice.toFixed(2)}</div><div style="font-size: 12px; margin-top: 2px;">${countdownText}</div>`;
            countdownEl.style.top = `${coordinate - 20}px`;
            countdownEl.style.display = 'block';

            if (latestCandle.close >= latestCandle.open) {
                countdownEl.style.background = '#26a69a';
            } else {
                countdownEl.style.background = '#ef5350';
            }
        }
    } catch (error) {
        console.error('Error updating price countdown:', error);
    }
}

function redrawChart() {
    candleSeries.setData(state.allCandles);
    updatePriceCountdown();
    updateCountdown();
}

async function init() {
    console.log('Initializing...');

    if (typeof LightweightCharts === 'undefined') {
        console.error('LightweightCharts library not loaded!');
        alert('Error: Chart library failed to load.');
        return;
    }

    const chartContainer = document.getElementById('chart');
    if (!chartContainer) {
        console.error('Chart container not found!');
        return;
    }

    try {
        chart = LightweightCharts.createChart(chartContainer, {
            layout: {
                background: { color: '#1a1a2e' },
                textColor: '#e0e0e0',
            },
            grid: {
                vertLines: { color: '#2a2a3e' },
                horzLines: { color: '#2a2a3e' },
            },
            timeScale: {
                timeVisible: true,
                secondsVisible: true,
                borderColor: '#2a2a3e',
            },
            rightPriceScale: {
                borderColor: '#2a2a3e',
            },
            localization: {
                timeFormatter: (timestamp) => formatTime(timestamp),
            },
            crosshair: {
                mode: LightweightCharts.CrosshairMode.Normal,
            },
            width: chartContainer.clientWidth,
            height: chartContainer.clientHeight,
        });

        candleSeries = chart.addCandlestickSeries({
            upColor: '#26a69a',
            downColor: '#ef5350',
            borderVisible: false,
            wickUpColor: '#26a69a',
            wickDownColor: '#ef5350',
        });

        chart.subscribeCrosshairMove((param) => {
            if (param.time) {
                const timeStr = formatTime(param.time);
                const data = param.seriesData.get(candleSeries);

                let ohlcText = timeStr;
                if (data) {
                    ohlcText = `${timeStr}\nO: ${data.open.toFixed(2)} H: ${data.high.toFixed(2)} L: ${data.low.toFixed(2)} C: ${data.close.toFixed(2)}`;
                }

                chart.applyOptions({
                    watermark: {
                        visible: true,
                        fontSize: 12,
                        horzAlign: 'left',
                        vertAlign: 'top',
                        color: 'rgba(255, 255, 255, 0.5)',
                        text: ohlcText,
                    },
                });
            }
        });

        console.log('Chart created successfully');

    } catch (error) {
        console.error('Error creating chart:', error);
        alert('Error creating chart: ' + error.message);
        return;
    }

    window.addEventListener('resize', () => {
        if (chart && chartContainer) {
            chart.applyOptions({
                width: chartContainer.clientWidth,
                height: chartContainer.clientHeight,
            });
            updatePriceCountdown();
        }
    });

    // Timeframe button click handlers
    document.querySelectorAll('.timeframe-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
            document.querySelectorAll('.timeframe-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const newInterval = btn.dataset.interval;
            await changeInterval(newInterval);
        });
    });

    // Load historical kLines first
    await loadKLines(state.currentInterval);

    // Then connect to live price stream
    console.log('Connecting to SSE endpoint...');
    eventSource = connectSSE();

    updateCountdown();
    state.countdownId = setInterval(updateCountdown, 1000);
}

async function changeInterval(interval) {
    console.log('Changing interval to', interval);
    state.currentInterval = interval;
    
    // Reload kLines for new interval
    await loadKLines(interval);
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
