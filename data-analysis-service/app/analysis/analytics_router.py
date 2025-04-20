from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import HTMLResponse
from sqlalchemy.orm import Session
from .database import get_db
from .analytics import (
    ClientSegmentation,
    LoanRecommendation,
    ProductUsageAnalytics,
    ClientValueAnalysis,
    ChurnPrediction,
    CreditScoring
)
from .visualizations import (
    create_credit_score_visualization,
    create_client_value_visualization,
    create_churn_risk_visualization,
    create_product_usage_visualization,
    create_client_segments_visualization,
    create_loan_recommendation_visualization,
    create_client_insights_visualization
)

router = APIRouter(prefix="/analytics", tags=["analytics"])


def create_html_response(data, visualizations):
    """Create an HTML response with data and visualizations"""
    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Analytics Dashboard</title>
        <script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
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
                display: flex;
                flex-direction: column;
            }}
            .card h2 {{ 
                margin-top: 0;
                margin-bottom: 15px;
                color: #333;
                border-bottom: 2px solid #eee;
                padding-bottom: 10px;
                font-size: 20px;
            }}
            .data-section {{ 
                margin-top: 20px;
                background: white;
                padding: 20px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                max-width: 1400px;
                margin-left: auto;
                margin-right: auto;
            }}
            pre {{ 
                background: #f8f9fa;
                padding: 15px;
                border-radius: 5px;
                overflow-x: auto;
            }}
            .plotly-graph-div {{ 
                width: 100% !important;
                height: 350px !important;
                margin: 0 auto;
            }}
            h1 {{
                max-width: 1400px;
                margin-left: auto;
                margin-right: auto;
                margin-bottom: 20px;
                color: #333;
            }}
            @media (max-width: 1024px) {{
                .container {{
                    grid-template-columns: 1fr;
                }}
            }}
        </style>
    </head>
    <body>
        <h1>Analytics Dashboard</h1>
        <div class="container">
            {visualizations}
        </div>
        <div class="data-section">
            <h2>Raw Data</h2>
            <pre>{data}</pre>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=html_content)


@router.get("/client-segments", response_class=HTMLResponse)
def get_client_segments(n_clusters: int = 5, db: Session = Depends(get_db)):
    """Get client segments based on clustering analysis with visualization"""
    try:
        segmentation = ClientSegmentation(db)
        result = segmentation.perform_clustering(n_clusters)
        
        visualizations = create_client_segments_visualization(result)
        if visualizations:
            viz_html = f"""
                <div class="card">
                    <h2>Client Segments Overview</h2>
                    <p class="description">
                        Client segmentation groups customers based on their financial behavior and characteristics.
                        Each segment represents a distinct pattern in banking activity, helping us understand different
                        client profiles and their needs.
                    </p>
                    <div class="segment-insights">
                        <h3>Key Insights:</h3>
                        <ul>
                            <li><strong>Balance-based Groups:</strong> Segments show different levels of account balance maintenance</li>
                            <li><strong>Transaction Patterns:</strong> Each group has distinct transaction frequencies and amounts</li>
                            <li><strong>Product Usage:</strong> Different segments show varying levels of engagement with banking products</li>
                            <li><strong>Activity Levels:</strong> Groups range from highly active to minimal engagement</li>
                        </ul>
                    </div>
                </div>
                <div class="card">
                    <h2>Cluster Statistics</h2>
                    <p class="description">
                        The chart below shows key metrics for each client segment, including average balance,
                        transaction frequency, and product usage. Higher values indicate stronger engagement
                        in that particular aspect.
                    </p>
                    {visualizations['clusters']}
                </div>
                <div class="card">
                    <h2>Segment Distribution</h2>
                    <p class="description">
                        This chart shows how clients are distributed across different segments.
                        The size of each segment indicates the number of clients with similar banking behavior patterns.
                    </p>
                    {visualizations['distribution']}
                </div>
                <div class="card">
                    <h2>Segment Characteristics</h2>
                    <div class="segment-details">
                        <h3>Typical Segment Profiles:</h3>
                        <ul>
                            <li><strong>High-Value Segment:</strong> Clients with high balances, frequent transactions, and multiple products</li>
                            <li><strong>Active Traders:</strong> Regular transaction activity but moderate balances</li>
                            <li><strong>Stable Savers:</strong> High balances but lower transaction frequency</li>
                            <li><strong>Basic Users:</strong> Lower balances and minimal product usage</li>
                            <li><strong>Occasional Users:</strong> Infrequent activity across all metrics</li>
                        </ul>
                    </div>
                </div>
            """
            return create_html_response(str(result), viz_html)
        return create_html_response(str(result), "")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/loan-recommendation/{client_id}", response_class=HTMLResponse)
def get_loan_recommendation(client_id: int, db: Session = Depends(get_db)):
    """Get loan recommendations for a specific client with visualization"""
    try:
        recommender = LoanRecommendation(db)
        result = recommender.recommend_loan(client_id)
        if not result:
            raise HTTPException(status_code=404, detail="Client not found")
        
        visualizations = create_loan_recommendation_visualization(result)
        if visualizations:
            viz_html = f"""
                <div class="card">
                    <h2>Loan Recommendations</h2>
                    {visualizations['recommendations']}
                </div>
                <div class="card">
                    <h2>Eligibility Score</h2>
                    {visualizations['eligibility']}
                </div>
            """
            return create_html_response(str(result), viz_html)
        return create_html_response(str(result), "")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/product-usage", response_class=HTMLResponse)
def get_product_usage(db: Session = Depends(get_db)):
    """Get product usage statistics with visualization"""
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
                    <h2>Product Combinations</h2>
                    {visualizations['combinations']}
                </div>
                <div class="card">
                    <h2>Usage Statistics</h2>
                    {visualizations['stats']}
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
