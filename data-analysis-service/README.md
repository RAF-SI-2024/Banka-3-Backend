# Bank Data Analysis Service

A service for analyzing bank data and providing insights through various analytical features.

## Features

- Client Segmentation
- Loan Recommendations
- Product Usage Statistics
- Client Lifetime Value Calculation
- Churn Risk Prediction
- Loan Default Risk Prediction
- Credit Score Estimation

## Setup

1. Clone the repository
2. Create a virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Copy the environment file:
   ```bash
   cp .env.example .env
   ```

5. Update the `.env` file with your configuration

6. Initialize the database:
   ```bash
   python -m app.analysis.database
   ```

## Running the Service

Start the service with:
```bash
uvicorn app.main:app --reload
```

The service will be available at `http://localhost:8000`

## API Endpoints

### Analysis Endpoints

- `GET /api/analysis/segmentation` - Get client segmentation
- `GET /api/analysis/loans/recommendations/{client_id}` - Get loan recommendations
- `GET /api/analysis/products/usage` - Get product usage statistics
- `GET /api/analysis/clients/{client_id}/lifetime-value` - Get client lifetime value
- `GET /api/analysis/clients/{client_id}/churn-risk` - Get churn risk prediction
- `GET /api/analysis/loans/default-risk/{client_id}` - Get loan default risk
- `GET /api/analysis/clients/{client_id}/credit-score` - Get credit score estimation

### Health Check

- `GET /` - Root endpoint
- `GET /health` - Health check endpoint

## API Documentation

Once the service is running, you can access the API documentation at:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

## Development

### Project Structure

```
data-analysis-service/
├── app/
│   ├── api/
│   │   └── analysis_routes.py
│   ├── analysis/
│   │   ├── analysis_service.py
│   │   ├── database.py
│   │   └── models.py
│   └── main.py
├── .env.example
├── requirements.txt
└── README.md
```

### Adding New Features

1. Add new analysis methods to `analysis_service.py`
2. Create corresponding API endpoints in `analysis_routes.py`
3. Add necessary database models in `models.py`
4. Update the README with new endpoint documentation

## License

MIT 