console.log('Script loaded!');

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
        // Attempt to reconnect after 5 seconds
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
    const time = Math.floor(Date.now() / 1000);

    document.getElementById('current-price').textContent = `$${data.middle}`;
    document.getElementById('resistance-level').textContent = `$${data.top}`;
    document.getElementById('support-level').textContent = `$${data.bottom}`;
    document.getElementById('data-count').textContent = `${state.dataIndex + 1}`;

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

    const newCandle = {
        time: time,
        open: price,
        high: price,
        low: price,
        close: price
    };
    state.allCandles.push(newCandle);

    redrawChart();
    state.dataIndex++;
}

let chart = null;
let candleSeries = null;
let resistanceLine = null;
let supportLine = null;
let eventSource = null;

const state = {
    dataIndex: 0,
    allCandles: [],
    timeframeMinutes: 1,
    countdownId: null,
    startTime: Date.now()
};

function getTicksPerCandle() {
    return state.timeframeMinutes * 60;
}

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


function updateCountdown() {
    const ticksPerCandle = getTicksPerCandle();
    const ticksInCurrentCandle = state.dataIndex % ticksPerCandle;
    const secondsRemaining = ticksPerCandle - ticksInCurrentCandle;

    const minutes = Math.floor(secondsRemaining / 60);
    const seconds = secondsRemaining % 60;

    document.getElementById('countdown-display').textContent =
        `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

function aggregateCandles(timeframeMinutes) {
    const ticksPerCandle = timeframeMinutes * 60;
    const aggregated = [];

    for (let i = 0; i < state.allCandles.length; i += ticksPerCandle) {
        const chunk = state.allCandles.slice(i, i + ticksPerCandle);
        if (chunk.length === 0) continue;

        const open = chunk[0].open;
        const close = chunk[chunk.length - 1].close;
        const high = Math.max(...chunk.map(c => c.high));
        const low = Math.min(...chunk.map(c => c.low));
        const time = chunk[0].time;

        aggregated.push({ time, open, high, low, close });
    }

    return aggregated;
}

function updatePriceCountdown() {
    const countdownEl = document.getElementById('price-countdown');
    if (!countdownEl) return;

    const aggregated = aggregateCandles(state.timeframeMinutes);
    if (aggregated.length === 0) {
        countdownEl.style.display = 'none';
        return;
    }

    const ticksPerCandle = getTicksPerCandle();
    const ticksInCurrentCandle = state.dataIndex % ticksPerCandle;
    const secondsRemaining = ticksPerCandle - ticksInCurrentCandle;
    const minutes = Math.floor(secondsRemaining / 60);
    const seconds = secondsRemaining % 60;
    const countdownText = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

    const latestCandle = aggregated[aggregated.length - 1];
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
    const aggregated = aggregateCandles(state.timeframeMinutes);
    candleSeries.setData(aggregated);
    updatePriceCountdown();
    updateCountdown();
}


function init() {
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
                chart.applyOptions({
                    watermark: {
                        visible: true,
                        fontSize: 12,
                        horzAlign: 'left',
                        vertAlign: 'top',
                        color: 'rgba(255, 255, 255, 0.5)',
                        text: timeStr,
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

    document.querySelectorAll('.timeframe-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.timeframe-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const newTimeframe = parseInt(btn.dataset.timeframe);
            changeTimeframe(newTimeframe);
        });
    });

    console.log('Connecting to SSE endpoint...');
    eventSource = connectSSE();
    
    updateCountdown();
    state.countdownId = setInterval(updateCountdown, 1000);
}

function changeTimeframe(minutes) {
    console.log('Changing timeframe to', minutes, 'minutes');
    state.timeframeMinutes = minutes;
    redrawChart();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
