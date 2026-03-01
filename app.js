console.log('Script loaded!');

// Generate mock data: 1800 items (30 minutes at 1s intervals)
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

// Application state
const state = {
    mockData: [],
    dataIndex: 0,
    allCandles: [],
    timeframeMinutes: 1,
    intervalId: null,
    countdownId: null,
    startTime: Date.now()
};

// Calculate ticks per candle based on timeframe
function getTicksPerCandle() {
    return state.timeframeMinutes * 60;
}

// Format time for display
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

// Update countdown timer
function updateCountdown() {
    const ticksPerCandle = getTicksPerCandle();
    const ticksInCurrentCandle = state.dataIndex % ticksPerCandle;
    const secondsRemaining = ticksPerCandle - ticksInCurrentCandle;
    
    const minutes = Math.floor(secondsRemaining / 60);
    const seconds = secondsRemaining % 60;
    
    document.getElementById('countdown-display').textContent = 
        `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

// Aggregate 1-second candles into larger timeframe
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

// Update countdown display on price scale
function updatePriceCountdown() {
    const countdownEl = document.getElementById('price-countdown');
    if (!countdownEl) return;
    
    const aggregated = aggregateCandles(state.timeframeMinutes);
    if (aggregated.length === 0) {
        countdownEl.style.display = 'none';
        return;
    }
    
    // Calculate countdown
    const ticksPerCandle = getTicksPerCandle();
    const ticksInCurrentCandle = state.dataIndex % ticksPerCandle;
    const secondsRemaining = ticksPerCandle - ticksInCurrentCandle;
    const minutes = Math.floor(secondsRemaining / 60);
    const seconds = secondsRemaining % 60;
    const countdownText = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    
    // Get current price
    const latestCandle = aggregated[aggregated.length - 1];
    const currentPrice = latestCandle.close;
    
    // Calculate position based on price
    try {
        const priceScale = chart.priceScale('right');
        const y = priceScale.priceToCoordinate(currentPrice);
        
        if (y !== null && y !== undefined) {
            countdownEl.textContent = countdownText;
            countdownEl.style.top = `${y + 25}px`;
            countdownEl.style.display = 'block';
            
            // Change color based on candle direction
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

// Redraw chart with current timeframe
function redrawChart() {
    const aggregated = aggregateCandles(state.timeframeMinutes);
    candleSeries.setData(aggregated);
    
    // Update countdown display on price scale
    updatePriceCountdown();
    
    // Update countdown
    updateCountdown();
}

// Initialize everything
function init() {
    console.log('Initializing...');
    
    // Generate mock data
    state.mockData = generateMockData();
    console.log('Mock data generated:', state.mockData.length, 'items');
    
    // Check if library is loaded
    if (typeof LightweightCharts === 'undefined') {
        console.error('LightweightCharts library not loaded!');
        alert('Error: Chart library failed to load. Check your internet connection.');
        return;
    }
    
    const chartContainer = document.getElementById('chart');
    if (!chartContainer) {
        console.error('Chart container not found!');
        return;
    }
    
    // Initialize chart
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
                timeFormatter: (timestamp) => {
                    return formatTime(timestamp);
                },
            },
            crosshair: {
                mode: LightweightCharts.CrosshairMode.Normal,
            },
            width: chartContainer.clientWidth,
            height: chartContainer.clientHeight,
        });
        
        // Add candlestick series
        candleSeries = chart.addCandlestickSeries({
            upColor: '#26a69a',
            downColor: '#ef5350',
            borderVisible: false,
            wickUpColor: '#26a69a',
            wickDownColor: '#ef5350',
        });
        
        // Subscribe to crosshair move for tooltip
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
    
    // Handle window resize
    window.addEventListener('resize', () => {
        if (chart && chartContainer) {
            chart.applyOptions({
                width: chartContainer.clientWidth,
                height: chartContainer.clientHeight,
            });
            updatePriceCountdown();
        }
    });
    
    // Setup timeframe buttons
    document.querySelectorAll('.timeframe-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            // Update active state
            document.querySelectorAll('.timeframe-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            // Change timeframe
            const newTimeframe = parseInt(btn.dataset.timeframe);
            changeTimeframe(newTimeframe);
        });
    });
    
    // Start updating
    console.log('Starting data feed...');
    startDataFeed();
}

// Change timeframe without resetting data
function changeTimeframe(minutes) {
    console.log('Changing timeframe to', minutes, 'minutes');
    
    state.timeframeMinutes = minutes;
    
    // Redraw chart with new timeframe
    redrawChart();
}

// Start data feed
function startDataFeed() {
    updateChart();
    state.intervalId = setInterval(updateChart, 1000);
    
    updateCountdown();
    state.countdownId = setInterval(updateCountdown, 1000);
}

// Update UI and chart
function updateChart() {
    try {
        if (state.dataIndex >= state.mockData.length) {
            console.log('All 1800 data points processed. Stopping updates.');
            clearInterval(state.intervalId);
            clearInterval(state.countdownId);
            return;
        }
        
        const current = state.mockData[state.dataIndex];
        const price = current.middle;
        const time = Math.floor((state.startTime + state.dataIndex * 1000) / 1000);
        
        // Update config panel
        document.getElementById('current-price').textContent = `$${current.middle}`;
        document.getElementById('resistance-level').textContent = `$${current.top}`;
        document.getElementById('support-level').textContent = `$${current.bottom}`;
        document.getElementById('data-count').textContent = `${state.dataIndex + 1} / 1800`;
        
        // Update resistance line with 50% opacity
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
        
        // Update support line with 50% opacity
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
        
        // Store as 1-second candle
        const newCandle = {
            time: time,
            open: price,
            high: price,
            low: price,
            close: price
        };
        state.allCandles.push(newCandle);
        
        // Redraw chart with current timeframe
        redrawChart();
        
        state.dataIndex++;
        
    } catch (error) {
        console.error('Error in updateChart:', error);
    }
}

// Wait for everything to load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
