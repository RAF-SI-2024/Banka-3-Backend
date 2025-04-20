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


# DONE
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


def generate_segment_insights(segments_data):
    """Generate insights for each segment based on their characteristics"""
    characteristics = segments_data['segment_characteristics']
    insights = []
    
    for segment_id, char in characteristics.items():
        segment_insight = f"""
        <div class="segment-description">
            <h3>Segment {segment_id}</h3>
            <p>This segment represents {char['size']} clients with the following characteristics:</p>
            <div class="metrics">
                <span class="metric">Balance Level: {char['balance_level']}</span>
                <span class="metric">Activity Level: {char['activity_level']}</span>
                <span class="metric">Card Usage: {char['card_usage']}</span>
                <span class="metric">Credit Card Usage: {char['credit_card_usage']}</span>
                <span class="metric">International Activity: {char['international_activity']}</span>
            </div>
            <p><strong>Recommendations:</strong></p>
            <ul>
                {generate_segment_recommendations(char)}
            </ul>
        </div>
        """
        insights.append(segment_insight)
    
    return "\n".join(insights)


def generate_segment_recommendations(characteristics):
    """Generate specific recommendations based on segment characteristics"""
    recommendations = []
    
    # Balance-based recommendations
    if characteristics['balance_level'] in ['High', 'Very High']:
        recommendations.append("Offer premium investment products and wealth management services")
    elif characteristics['balance_level'] in ['Low', 'Very Low']:
        recommendations.append("Consider financial education programs and basic banking solutions")
    
    # Activity-based recommendations
    if characteristics['activity_level'] == 'High':
        recommendations.append("Introduce transaction fee packages and cashback rewards")
    elif characteristics['activity_level'] == 'Low':
        recommendations.append("Promote mobile banking features to increase engagement")
    
    # Card usage recommendations
    if characteristics['card_usage'] == 'Multiple':
        recommendations.append("Offer premium credit cards with rewards programs")
    elif characteristics['card_usage'] == 'None':
        recommendations.append("Promote card-based services and digital payment solutions")
    
    # International activity recommendations
    if characteristics['international_activity'] == 'Yes':
        recommendations.append("Offer foreign currency accounts and international transfer services")
    
    # Credit card specific recommendations
    if characteristics['credit_card_usage'] == 'High':
        recommendations.append("Consider card-based insurance products and travel benefits")
    
    # Format recommendations as list items
    return "\n".join([f"<li>{rec}</li>" for rec in recommendations])


# DONE
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


# DONE
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


# DONE
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


# DONE WHEN EVERYTHING
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
