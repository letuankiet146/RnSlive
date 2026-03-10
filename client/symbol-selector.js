/**
 * Dynamic Symbol Selector Component
 * Fetches symbols from Binance Futures API and provides search functionality
 */

class SymbolSelector {
    constructor(selectElementId, options = {}) {
        this.selectElement = document.getElementById(selectElementId);
        this.symbols = [];
        this.filteredSymbols = [];
        this.onSymbolChange = options.onSymbolChange || (() => {});
        this.defaultSymbol = options.defaultSymbol || 'BTCUSDT';
        this.apiUrl = 'https://fapi.binance.com/fapi/v1/exchangeInfo';
        
        this.init();
    }

    async init() {
        await this.fetchSymbols();
        this.createSearchableDropdown();
        this.attachEventListeners();
    }

    async fetchSymbols() {
        try {
            const response = await fetch(this.apiUrl);
            const data = await response.json();
            
            // Filter for PERPETUAL contracts with TRADING status
            this.symbols = data.symbols
                .filter(s => s.contractType === 'PERPETUAL' && s.status === 'TRADING')
                .map(s => {
                    const filters = s.filters.reduce(
                        (acc, filter) => ({ ...acc, [filter.filterType]: filter }),
                        {}
                    );
                    return {
                        symbol: s.symbol,
                        baseAsset: s.baseAsset,
                        quoteAsset: s.quoteAsset,
                        pricePrecision: s.pricePrecision,
                        quantityPrecision: s.quantityPrecision,
                        tickSize: Number(filters.PRICE_FILTER.tickSize),
                        stepSize: Number(filters.LOT_SIZE.stepSize),
                        filters
                    };
                })
                .sort((a, b) => a.symbol.localeCompare(b.symbol));
            
            this.filteredSymbols = [...this.symbols];
            console.log(`Loaded ${this.symbols.length} trading symbols`);
        } catch (error) {
            console.error('Error fetching symbols:', error);
            // Fallback to default symbols
            this.symbols = [
                { symbol: 'BTCUSDT', baseAsset: 'BTC', quoteAsset: 'USDT' },
                { symbol: 'XRPUSDT', baseAsset: 'XRP', quoteAsset: 'USDT' }
            ];
            this.filteredSymbols = [...this.symbols];
        }
    }

    createSearchableDropdown() {
        const parent = this.selectElement.parentElement;
        const wrapper = document.createElement('div');
        wrapper.className = 'symbol-selector-wrapper';
        wrapper.style.cssText = 'position: relative; display: inline-block; width: 100%;';
        
        // Create dropdown container
        const dropdownContainer = document.createElement('div');
        dropdownContainer.className = 'symbol-dropdown-container';
        dropdownContainer.style.cssText = `
            position: relative;
            width: 100%;
        `;
        
        // Create custom select button
        const selectButton = document.createElement('button');
        selectButton.type = 'button';
        selectButton.className = 'symbol-select-button';
        selectButton.style.cssText = `
            width: 100%;
            padding: 8px 12px;
            background: #2a2a3e;
            color: #e0e0e0;
            border: 1px solid #3a3a4e;
            border-radius: 4px;
            cursor: pointer;
            text-align: left;
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 14px;
        `;
        selectButton.innerHTML = `<span>${this.defaultSymbol}</span><span>▼</span>`;
        
        // Create dropdown list container
        const dropdownList = document.createElement('div');
        dropdownList.className = 'symbol-dropdown-list';
        dropdownList.style.cssText = `
            display: none;
            position: absolute;
            top: 100%;
            left: 0;
            right: 0;
            background: #2a2a3e;
            border: 1px solid #3a3a4e;
            border-radius: 4px;
            margin-top: 4px;
            z-index: 1000;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
        `;
        
        // Create search input (inside dropdown)
        const searchInput = document.createElement('input');
        searchInput.type = 'text';
        searchInput.placeholder = 'Search symbol...';
        searchInput.className = 'symbol-search-input';
        searchInput.style.cssText = `
            width: 100%;
            padding: 10px;
            background: #1e2329;
            color: #e0e0e0;
            border: none;
            border-bottom: 1px solid #3a3a4e;
            box-sizing: border-box;
            font-size: 14px;
            outline: none;
        `;
        
        // Create scrollable options container
        const optionsContainer = document.createElement('div');
        optionsContainer.className = 'symbol-options-container';
        optionsContainer.style.cssText = `
            max-height: 300px;
            overflow-y: auto;
        `;
        
        // Populate dropdown
        this.populateDropdown(optionsContainer, selectButton);
        
        // Assemble dropdown
        dropdownList.appendChild(searchInput);
        dropdownList.appendChild(optionsContainer);
        
        // Assemble components
        dropdownContainer.appendChild(selectButton);
        dropdownContainer.appendChild(dropdownList);
        wrapper.appendChild(dropdownContainer);
        
        // Replace original select
        parent.insertBefore(wrapper, this.selectElement);
        this.selectElement.style.display = 'none';
        
        // Store references
        this.searchInput = searchInput;
        this.selectButton = selectButton;
        this.dropdownList = dropdownList;
        this.optionsContainer = optionsContainer;
        this.wrapper = wrapper;
    }

    populateDropdown(optionsContainer, selectButton) {
        optionsContainer.innerHTML = '';
        
        if (this.filteredSymbols.length === 0) {
            const noResults = document.createElement('div');
            noResults.style.cssText = 'padding: 12px; color: #848e9c; text-align: center;';
            noResults.textContent = 'No symbols found';
            optionsContainer.appendChild(noResults);
            return;
        }
        
        this.filteredSymbols.forEach(symbolData => {
            const option = document.createElement('div');
            option.className = 'symbol-option';
            option.style.cssText = `
                padding: 10px 12px;
                cursor: pointer;
                color: #e0e0e0;
                transition: background 0.2s;
                font-size: 14px;
            `;
            option.textContent = symbolData.symbol;
            option.dataset.symbol = symbolData.symbol;
            
            option.addEventListener('mouseenter', () => {
                option.style.background = '#3a3a4e';
            });
            
            option.addEventListener('mouseleave', () => {
                option.style.background = 'transparent';
            });
            
            option.addEventListener('click', () => {
                const tickSize = Number(symbolData.filters.PRICE_FILTER.tickSize);
                this.selectSymbol(symbolData.symbol, symbolData.pricePrecision, tickSize, selectButton);
            });
            
            optionsContainer.appendChild(option);
        });
    }

    selectSymbol(symbol, pricePrecision, tickSize, selectButton) {
        selectButton.querySelector('span').textContent = symbol;
        this.selectElement.value = symbol;
        this.dropdownList.style.display = 'none';
        this.searchInput.value = '';
        this.filteredSymbols = [...this.symbols];
        this.onSymbolChange(symbol, pricePrecision, tickSize);
    }

    attachEventListeners() {
        // Toggle dropdown and focus search
        this.selectButton.addEventListener('click', (e) => {
            e.stopPropagation();
            const isVisible = this.dropdownList.style.display === 'block';
            
            if (isVisible) {
                this.dropdownList.style.display = 'none';
            } else {
                // Reset search and filtered symbols when opening dropdown
                this.searchInput.value = '';
                this.filteredSymbols = [...this.symbols];
                this.populateDropdown(this.optionsContainer, this.selectButton);
                this.dropdownList.style.display = 'block';
                
                // Focus search input when dropdown opens
                setTimeout(() => this.searchInput.focus(), 50);
            }
        });
        
        // Search functionality with live results
        this.searchInput.addEventListener('input', (e) => {
            const searchTerm = e.target.value.toUpperCase();
            this.filteredSymbols = this.symbols.filter(s => 
                s.symbol.includes(searchTerm) || 
                s.baseAsset.includes(searchTerm)
            );
            this.populateDropdown(this.optionsContainer, this.selectButton);
        });
        
        // Prevent dropdown from closing when clicking search input
        this.searchInput.addEventListener('click', (e) => {
            e.stopPropagation();
        });
        
        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!this.wrapper.contains(e.target)) {
                this.dropdownList.style.display = 'none';
                this.searchInput.value = '';
                this.filteredSymbols = [...this.symbols];
            }
        });
        
        // Keyboard navigation
        this.searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && this.filteredSymbols.length > 0) {
                this.selectSymbol(this.filteredSymbols[0].symbol, this.filteredSymbols[0].pricePrecision, this.selectButton);
            } else if (e.key === 'Escape') {
                this.dropdownList.style.display = 'none';
                this.searchInput.value = '';
                this.filteredSymbols = [...this.symbols];
            }
        });
    }

    getSelectedSymbol() {
        return this.selectElement.value || this.defaultSymbol;
    }

    getAllSymbols() {
        return this.symbols;
    }
}
