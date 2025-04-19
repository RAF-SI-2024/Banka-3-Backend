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
            body {{ font-family: Arial, sans-serif; margin: 20px; }}
            .container {{ display: flex; flex-wrap: wrap; gap: 20px; }}
            .card {{ flex: 1; min-width: 300px; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }}
            .data-section {{ margin-top: 20px; }}
            pre {{ background: #f5f5f5; padding: 10px; border-radius: 5px; }}
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
                    <h2>Cluster Statistics</h2>
                    {visualizations['clusters']}
                </div>
                <div class="card">
                    <h2>Segment Distribution</h2>
                    {visualizations['distribution']}
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
