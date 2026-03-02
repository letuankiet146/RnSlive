console.log('Script loaded!');

function generateMockData() {
    const data = [];
    let basePrice = 100;
    
    for (let i = 0; i < 1800; i++) {
        const variation = (Math.random() - 0.5) * 2;
        const currentPrice = basePrice + variation;
        const resistance = currentPrice + Math.random() * 5 + 2;
        const support = currentPrice - Math.random() * 5 - 2;
        
        data.push({
            top: parseFloat(resistance.toFixed(2)),
            middle: parseFloat(currentPrice.toFixed(2)),
            bottom: parseFloat(support.toFixed(2))
        });
        
        basePrice = currentPrice;
    }
    
    return data;
}

let chart = null;
let candleSeries = null;
let resistanceLine = null;
let supportLine = null;

const state = {
    mockData: [],
    dataIndex: 0,
    allCandles: [],
    timeframeMinutes: 1,
    intervalId: null,
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
            // Display price and countdown together
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
    
    state.mockData = generateMockData();
    console.log('Mock data generated:', state.mockData.length, 'items');
    
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
    
    console.log('Starting data feed...');
    startDataFeed();
}

function changeTimeframe(minutes) {
    console.log('Changing timeframe to', minutes, 'minutes');
    state.timeframeMinutes = minutes;
    redrawChart();
}

function startDataFeed() {
    updateChart();
    state.intervalId = setInterval(updateChart, 1000);
    
    updateCountdown();
    state.countdownId = setInterval(updateCountdown, 1000);
}

function updateChart() {
    try {
        if (state.dataIndex >= state.mockData.length) {
            console.log('All 1800 data points processed.');
            clearInterval(state.intervalId);
            clearInterval(state.countdownId);
            return;
        }
        
        const current = state.mockData[state.dataIndex];
        const price = current.middle;
        const time = Math.floor((state.startTime + state.dataIndex * 1000) / 1000);
        
        document.getElementById('current-price').textContent = `$${current.middle}`;
        document.getElementById('resistance-level').textContent = `$${current.top}`;
        document.getElementById('support-level').textContent = `$${current.bottom}`;
        document.getElementById('data-count').textContent = `${state.dataIndex + 1} / 1800`;
        
        if (resistanceLine) {
            candleSeries.removePriceLine(resistanceLine);
        }
        resistanceLine = candleSeries.createPriceLine({
            price: current.top,
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
            price: current.bottom,
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
        
    } catch (error) {
        console.error('Error in updateChart:', error);
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
