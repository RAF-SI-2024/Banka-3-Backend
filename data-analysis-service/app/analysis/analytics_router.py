from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import HTMLResponse
from sqlalchemy.orm import Session
from sqlalchemy import select, func
from app.analysis.models import Account, Payment, Card, Loan

from .calculations.churn_risk import ChurnPrediction
from .calculations.client_segmentation import ClientSegmentation, generate_segment_insights
from .calculations.client_value import ClientValueAnalysis
from .calculations.credit_score import CreditScoring
from .calculations.loans import LoanRecommendation
from .calculations.product_usage import ProductUsageAnalytics
from .database import get_db
from .visuals.churn_risk import create_churn_risk_visualization
from .visuals.client_segmentation import create_client_segments_visualization
from .visuals.client_value import create_client_value_visualization
from .visuals.credit_score import create_credit_score_visualization
from .visuals.loans import create_loan_recommendation_visualization
from .visuals.product_usage import create_product_usage_visualization
from .visuals.visualizations import create_client_insights_visualization, create_html_response

router = APIRouter(prefix="/analytics", tags=["analytics"])


@router.get("/client-segments", response_class=HTMLResponse)
def get_client_segments(n_clusters: int = 5, db: Session = Depends(get_db)):
    """Get client segmentation analysis with visualizations"""
    try:
        # Initialize segmentation
        segmentation = ClientSegmentation(db)
        
        # Perform clustering
        segments_data = segmentation.perform_clustering(n_clusters=n_clusters)
        
        # Create visualizations
        visualizations = create_client_segments_visualization(segments_data)
        
        # Create HTML response with insights
        html_content = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Client Segmentation Analysis</title>
            <script src="https://cdn.plot.ly/plotly-2.27.0.min.js"></script>
            <style>
                body {{ 
                    font-family: Arial, sans-serif; 
                    margin: 20px; 
                    background-color: #f5f5f5;
                }}
                .container {{ 
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 20px;
                    margin-bottom: 20px;
                    max-width: 1400px;
                    margin-left: auto;
                    margin-right: auto;
                }}
                .card {{ 
                    background: white;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }}
                .insights {{ 
                    grid-column: span 2;
                    background: white;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    margin-top: 20px;
                }}
                h1, h2, h3 {{ color: #333; }}
                .segment-description {{ margin-bottom: 15px; }}
                .metric {{ 
                    display: inline-block;
                    margin-right: 20px;
                    padding: 5px 10px;
                    background: #f0f0f0;
                    border-radius: 4px;
                }}
            </style>
        </head>
        <body>
            <h1>Client Segmentation Analysis</h1>
            <p>Analysis of client segments based on their banking behavior and characteristics.</p>
            
            {visualizations}
            
            <div class="insights">
                <h2>Segment Insights</h2>
                {generate_segment_insights(segments_data)}
            </div>
        </body>
        </html>
        """
        
        return HTMLResponse(content=html_content)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/loan-recommendation/{client_id}")
async def get_loan_recommendation(client_id: int, db: Session = Depends(get_db)):
    """Get loan recommendations for a client with visualizations"""
    try:
        # Initialize loan recommendation
        recommender = LoanRecommendation(db)
        
        # Get loan recommendation data
        result = recommender.recommend_loan(client_id)
        if not result:
            raise HTTPException(status_code=404, detail="Could not generate loan recommendations")
            
        # Create visualizations
        visualizations = create_loan_recommendation_visualization(result)
        if not visualizations:
            raise HTTPException(status_code=500, detail="Error creating visualizations")
            
        # Return the complete dashboard HTML
        return HTMLResponse(content=visualizations['dashboard_html'], status_code=200)
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/product-usage", response_class=HTMLResponse)
def get_product_usage(db: Session = Depends(get_db)):
    """Get product engagement analysis with visualization"""
    try:
        analytics = ProductUsageAnalytics(db)
        stats = analytics.get_product_usage_stats()
        combinations = analytics.get_product_combinations()
        result = {
            "usage_stats": stats,
            "product_combinations": combinations
        }

        visualizations = create_product_usage_visualization(result)
        if visualizations:
            viz_html = f"""
                <div class="card">
                    <h2>Product Engagement Combinations</h2>
                    {visualizations['combinations']}
                </div>
                <div class="card">
                    <h2>Engagement Statistics</h2>
                    {visualizations['stats']}
                </div>
                <div class="card">
                    <h2>Product Correlation Matrix</h2>
                    {visualizations['heatmap']}
                </div>
                <div class="card">
                    <h2>Product Adoption Trends</h2>
                    {visualizations['adoption']}
                </div>
                <div class="card">
                    <h2>Product Usage by Segment</h2>
                    {visualizations['segment']}
                </div>
                <div class="card">
                    <h2>Total Loan Statistics</h2>
                    {visualizations['total_loans']}
                </div>
                <div class="card">
                    <h2>Loans by Type</h2>
                    {visualizations['loans_by_type']}
                </div>
                <div class="card">
                    <h2>Loans by Status</h2>
                    {visualizations['loans_by_status']}
                </div>
            """
            return create_html_response(str(result), viz_html)
        return create_html_response(str(result), "")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/client-value/{client_id}", response_class=HTMLResponse)
def get_client_value(client_id: int, db: Session = Depends(get_db)):
    """Get client lifetime value analysis with visualization"""
    try:
        analyzer = ClientValueAnalysis(db)
        result = analyzer.calculate_client_value(client_id)
        if not result:
            raise HTTPException(status_code=404, detail="Client not found")

        visualizations = create_client_value_visualization(result)
        if visualizations:
            viz_html = f"""
                <div class="card">
                    <h2>Value Metrics</h2>
                    {visualizations['radar']}
                </div>
                <div class="card">
                    <h2>Total Value Score</h2>
                    {visualizations['gauge']}
                </div>
            """
            return create_html_response(str(result), viz_html)
        return create_html_response(str(result), "")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/churn-risk/{client_id}", response_class=HTMLResponse)
def get_churn_risk(client_id: int, db: Session = Depends(get_db)):
    """Get churn risk assessment for a client with visualization"""
    try:
        predictor = ChurnPrediction(db)
        result = predictor.get_churn_indicators(client_id)
        if not result:
            raise HTTPException(status_code=404, detail="Client not found")

        visualizations = create_churn_risk_visualization(result)
        if visualizations:
            viz_html = f"""
                <div class="card">
                    <h2>Churn Risk Score</h2>
                    {visualizations['gauge']}
                </div>
                <div class="card">
                    <h2>Risk Indicators</h2>
                    {visualizations['indicators']}
                </div>
            """
            return create_html_response(str(result), viz_html)
        return create_html_response(str(result), "")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/credit-score/{client_id}", response_class=HTMLResponse)
def get_credit_score(client_id: int, db: Session = Depends(get_db)):
    """Get internal credit score for a client with visualization"""
    try:
        scorer = CreditScoring(db)
        result = scorer.calculate_credit_score(client_id)
        if not result:
            raise HTTPException(status_code=404, detail="Client not found")

        visualizations = create_credit_score_visualization(result)
        if visualizations:
            viz_html = f"""
                <div class="card">
                    <h2>Credit Score</h2>
                    {visualizations['gauge']}
                </div>
                <div class="card">
                    <h2>Score Components</h2>
                    {visualizations['components']}
                </div>
            """
            return create_html_response(str(result), viz_html)
        return create_html_response(str(result), "")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/client-insights/{client_id}", response_class=HTMLResponse)
async def get_client_insights(client_id: int, db: Session = Depends(get_db)):
    """Get comprehensive insights for a client with visualization"""
    try:
        # Get all analytics for the client
        loan_rec = LoanRecommendation(db).recommend_loan(client_id)
        client_value = ClientValueAnalysis(db).calculate_client_value(client_id)
        churn_risk = ChurnPrediction(db).get_churn_indicators(client_id)
        credit_score = CreditScoring(db).calculate_credit_score(client_id)

        if not any([loan_rec, client_value, churn_risk, credit_score]):
            raise HTTPException(status_code=404, detail="Client not found")

        result = {
            "loan_recommendations": loan_rec,
            "client_value": client_value,
            "churn_risk": churn_risk,
            "credit_score": credit_score
        }

        visualizations = create_client_insights_visualization(result)
        if visualizations:
            return create_html_response(str(result), visualizations)
        return create_html_response(str(result), "")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/", response_class=HTMLResponse)
async def get_analytics_menu():
    """Display a menu of all available analytics endpoints"""
    menu_html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>Bank Analytics Dashboard</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
        <style>
            body {
                font-family: Arial, sans-serif;
                margin: 0;
                padding: 20px;
                background-color: #f8f9fa;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
            }
            .header {
                text-align: center;
                margin-bottom: 40px;
                padding: 20px;
                background-color: #fff;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            .card {
                margin-bottom: 20px;
                border: none;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                transition: transform 0.2s;
            }
            .card:hover {
                transform: translateY(-5px);
            }
            .card-body {
                padding: 20px;
            }
            .card-title {
                color: #2c3e50;
                margin-bottom: 15px;
            }
            .card-text {
                color: #7f8c8d;
            }
            .btn-primary {
                background-color: #3498db;
                border: none;
                padding: 8px 16px;
                border-radius: 4px;
                color: white;
                text-decoration: none;
                display: inline-block;
            }
            .btn-primary:hover {
                background-color: #2980b9;
            }
            .form-group {
                margin-bottom: 20px;
            }
            .form-control {
                padding: 8px;
                border-radius: 4px;
                border: 1px solid #ddd;
                width: 100%;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>Bank Analytics Dashboard</h1>
                <p class="lead">Select an analytics feature to explore</p>
            </div>
            
            <div class="row">
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Clients List</h5>
                            <p class="card-text">View all clients and their basic information.</p>
                            <a href="/analytics/clients" class="btn btn-primary">View Clients</a>
                        </div>
                    </div>
                </div>
                
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Client Insights</h5>
                            <p class="card-text">Get comprehensive insights about a specific client including credit score, value, churn risk, and loan recommendations.</p>
                            <form onsubmit="window.location.href='/analytics/client-insights/' + document.getElementById('client-insights-id').value; return false;" class="form-group">
                                <input type="number" id="client-insights-id" class="form-control" placeholder="Enter Client ID" required>
                                <button type="submit" class="btn btn-primary mt-2">View Insights</button>
                            </form>
                        </div>
                    </div>
                </div>
                
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Client Segments</h5>
                            <p class="card-text">View analysis of client segments based on their banking behavior and characteristics.</p>
                            <a href="/analytics/client-segments" class="btn btn-primary">View Segments</a>
                        </div>
                    </div>
                </div>
                
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Product Usage</h5>
                            <p class="card-text">View analysis of product engagement and usage patterns across all clients.</p>
                            <a href="/analytics/product-usage" class="btn btn-primary">View Usage</a>
                        </div>
                    </div>
                </div>
                
            </div>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=menu_html)


@router.get("/clients", response_class=HTMLResponse)
async def get_clients_list(db: Session = Depends(get_db)):
    """Display a list of all clients with basic information"""
    try:
        # Get unique client IDs from accounts, including both personal and company accounts
        client_ids = db.query(Account.client_id).filter(Account.client_id.isnot(None)).distinct().all()
        client_ids = [client_id[0] for client_id in client_ids]

        clients_data = []
        for client_id in client_ids:
            # Get accounts for this client, ensuring we get both personal and company accounts
            accounts = db.query(Account).filter(
                Account.client_id == client_id
            ).all()
            
            # Get all cards for this client's accounts
            account_numbers = [account.account_number for account in accounts]
            cards = db.query(Card).filter(Card.account_number.in_(account_numbers)).all()

            # Get loans for this client's accounts
            loans = db.query(Loan).filter(Loan.account_number.in_(account_numbers)).all()
            
            # Get transactions for this client's accounts
            transactions = db.query(Payment).filter(Payment.sender_account_number.in_(account_numbers)).all()
            
            # Calculate total balance across all accounts
            total_balance = sum(account.balance for account in accounts)
            
            clients_data.append({
                'client_id': client_id,
                'account_count': len(accounts),
                'total_balance': total_balance,
                'card_count': len(cards),
                'loan_count': len(loans),
                'transaction_count': len(transactions)
            })

        clients_html = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Client List</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
            <style>
                body {
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f8f9fa;
                }
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                }
                .header {
                    text-align: center;
                    margin-bottom: 40px;
                    padding: 20px;
                    background-color: #fff;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .client-card {
                    margin-bottom: 20px;
                    border: none;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    transition: transform 0.2s;
                }
                .client-card:hover {
                    transform: translateY(-5px);
                }
                .client-card .card-body {
                    padding: 20px;
                }
                .stats-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                    gap: 15px;
                    margin-top: 15px;
                }
                .stat-item {
                    text-align: center;
                    padding: 10px;
                    background-color: #f8f9fa;
                    border-radius: 4px;
                }
                .stat-value {
                    font-size: 1.2em;
                    font-weight: bold;
                    color: #3498db;
                }
                .stat-label {
                    font-size: 0.9em;
                    color: #7f8c8d;
                }
                .btn-primary {
                    background-color: #3498db;
                    border: none;
                    padding: 8px 16px;
                    border-radius: 4px;
                    color: white;
                    text-decoration: none;
                    display: inline-block;
                }
                .btn-primary:hover {
                    background-color: #2980b9;
                }
                .analytics-buttons {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
                    gap: 10px;
                    margin-top: 15px;
                }
                .analytics-button {
                    padding: 5px 10px;
                    font-size: 0.8em;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Client List</h1>
                    <p class="lead">Overview of all bank clients</p>
                    <div class="mt-3">
                        <a href="/analytics" class="btn btn-primary">Back to Dashboard</a>
                    </div>
                </div>
                
                <div class="row">
        """

        for client in clients_data:
            clients_html += f"""
                    <div class="col-md-6">
                        <div class="card client-card">
                            <div class="card-body">
                                <h5 class="card-title">Client #{client['client_id']}</h5>
                                <div class="stats-grid">
                                    <div class="stat-item">
                                        <div class="stat-value">{client['account_count']}</div>
                                        <div class="stat-label">Accounts</div>
                                    </div>
                                    <div class="stat-item">
                                        <div class="stat-value">${client['total_balance']:,.2f}</div>
                                        <div class="stat-label">Total Balance</div>
                                    </div>
                                    <div class="stat-item">
                                        <div class="stat-value">{client['card_count']}</div>
                                        <div class="stat-label">Cards</div>
                                    </div>
                                    <div class="stat-item">
                                        <div class="stat-value">{client['loan_count']}</div>
                                        <div class="stat-label">Loans</div>
                                    </div>
                                    <div class="stat-item">
                                        <div class="stat-value">{client['transaction_count']}</div>
                                        <div class="stat-label">Transactions</div>
                                    </div>
                                </div>
                                <div class="mt-3">
                                    <a href="/analytics/clients/{client['client_id']}" class="btn btn-primary">View Details</a>
                                </div>
                                <div class="analytics-buttons">
                                    <a href="/analytics/client-value/{client['client_id']}" class="btn btn-primary analytics-button">Value</a>
                                    <a href="/analytics/churn-risk/{client['client_id']}" class="btn btn-primary analytics-button">Churn Risk</a>
                                    <a href="/analytics/credit-score/{client['client_id']}" class="btn btn-primary analytics-button">Credit Score</a>
                                    <a href="/analytics/loan-recommendation/{client['client_id']}" class="btn btn-primary analytics-button">Loans</a>
                                    <a href="/analytics/client-insights/{client['client_id']}" class="btn btn-primary analytics-button">Full Insights</a>
                                </div>
                            </div>
                        </div>
                    </div>
            """

        clients_html += """
                </div>
            </div>
        </body>
        </html>
        """
        return HTMLResponse(content=clients_html)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/clients/{client_id}", response_class=HTMLResponse)
async def get_client_details(client_id: int, db: Session = Depends(get_db)):
    """Display detailed information about a specific client"""
    try:
        # Get client accounts
        accounts = db.query(Account).filter(Account.client_id == client_id).all()

        # Get client cards
        cards = db.query(Card).join(Account).filter(Account.client_id == client_id).all()

        # Get client loans
        loans = db.query(Loan).join(Account).filter(Account.client_id == client_id).all()

        # Get recent transactions
        recent_transactions = db.query(Payment).join(Account).filter(Account.client_id == client_id).order_by(Payment.date.desc()).limit(5).all()

        details_html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Client #{client_id} Details</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
            <style>
                body {{
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f8f9fa;
                }}
                .container {{
                    max-width: 1200px;
                    margin: 0 auto;
                }}
                .header {{
                    text-align: center;
                    margin-bottom: 40px;
                    padding: 20px;
                    background-color: #fff;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }}
                .section {{
                    margin-bottom: 30px;
                    padding: 20px;
                    background-color: #fff;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }}
                .table {{
                    width: 100%;
                    margin-bottom: 1rem;
                    color: #212529;
                    border-collapse: collapse;
                }}
                .table th,
                .table td {{
                    padding: 0.75rem;
                    vertical-align: top;
                    border-top: 1px solid #dee2e6;
                }}
                .table thead th {{
                    vertical-align: bottom;
                    border-bottom: 2px solid #dee2e6;
                }}
                .btn-primary {{
                    background-color: #3498db;
                    border: none;
                    padding: 8px 16px;
                    border-radius: 4px;
                    color: white;
                    text-decoration: none;
                    display: inline-block;
                }}
                .btn-primary:hover {{
                    background-color: #2980b9;
                }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Client #{client_id} Details</h1>
                    <div class="mt-3">
                        <a href="/analytics/clients" class="btn btn-primary">Back to Client List</a>
                    </div>
                </div>

                <div class="section">
                    <h2>Accounts</h2>
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Account Number</th>
                                <th>Type</th>
                                <th>Balance</th>
                                <th>Currency</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
        """

        for account in accounts:
            details_html += f"""
                            <tr>
                                <td>{account.account_number}</td>
                                <td>{account.type.value}</td>
                                <td>${account.balance:,.2f}</td>
                                <td>{account.currency_code}</td>
                                <td>{account.status.value}</td>
                            </tr>
            """

        details_html += """
                        </tbody>
                    </table>
                </div>

                <div class="section">
                    <h2>Cards</h2>
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Card Number</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>Limit</th>
                                <th>Expiration</th>
                            </tr>
                        </thead>
                        <tbody>
        """

        for card in cards:
            details_html += f"""
                            <tr>
                                <td>{card.card_number}</td>
                                <td>{card.type.value}</td>
                                <td>{card.status.value}</td>
                                <td>${card.card_limit:,.2f}</td>
                                <td>{card.expiration_date}</td>
                            </tr>
            """

        details_html += """
                        </tbody>
                    </table>
                </div>

                <div class="section">
                    <h2>Loans</h2>
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Loan Number</th>
                                <th>Type</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Start Date</th>
                                <th>Due Date</th>
                            </tr>
                        </thead>
                        <tbody>
        """

        for loan in loans:
            details_html += f"""
                            <tr>
                                <td>{loan.loan_number}</td>
                                <td>{loan.type.value}</td>
                                <td>${loan.amount:,.2f}</td>
                                <td>{loan.status.value}</td>
                                <td>{loan.start_date}</td>
                                <td>{loan.due_date}</td>
                            </tr>
            """

        details_html += """
                        </tbody>
                    </table>
                </div>

                <div class="section">
                    <h2>Recent Transactions</h2>
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Purpose</th>
                            </tr>
                        </thead>
                        <tbody>
        """

        for transaction in recent_transactions:
            details_html += f"""
                            <tr>
                                <td>{transaction.date}</td>
                                <td>${transaction.amount:,.2f}</td>
                                <td>{transaction.status.value}</td>
                                <td>{transaction.purpose_of_payment}</td>
                            </tr>
            """

        details_html += """
                        </tbody>
                    </table>
                </div>

                <div class="section">
                    <h2>Analytics</h2>
                    <div class="row">
                        <div class="col-md-4">
                            <a href="/analytics/client-value/{client_id}" class="btn btn-primary w-100 mb-2">Client Value</a>
                        </div>
                        <div class="col-md-4">
                            <a href="/analytics/churn-risk/{client_id}" class="btn btn-primary w-100 mb-2">Churn Risk</a>
                        </div>
                        <div class="col-md-4">
                            <a href="/analytics/credit-score/{client_id}" class="btn btn-primary w-100 mb-2">Credit Score</a>
                        </div>
                        <div class="col-md-4">
                            <a href="/analytics/loan-recommendation/{client_id}" class="btn btn-primary w-100 mb-2">Loan Recommendations</a>
                        </div>
                        <div class="col-md-4">
                            <a href="/analytics/client-insights/{client_id}" class="btn btn-primary w-100 mb-2">Full Insights</a>
                        </div>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """
        return HTMLResponse(content=details_html)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
