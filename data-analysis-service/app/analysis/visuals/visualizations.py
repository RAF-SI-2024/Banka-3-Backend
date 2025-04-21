from app.analysis.visuals.churn_risk import create_churn_risk_visualization
from app.analysis.visuals.client_value import create_client_value_visualization
from app.analysis.visuals.credit_score import create_credit_score_visualization
from app.analysis.visuals.loans import create_loan_recommendation_visualization
from fastapi.responses import HTMLResponse


def create_client_insights_visualization(insights_data):
    """Create a comprehensive dashboard for all client insights"""
    if not insights_data:
        return None

    # Create a grid of visualizations
    credit_score_viz = create_credit_score_visualization(insights_data['credit_score'])
    client_value_viz = create_client_value_visualization(insights_data['client_value'])
    churn_risk_viz = create_churn_risk_visualization(insights_data['churn_risk'])
    loan_rec_viz = create_loan_recommendation_visualization(insights_data['loan_recommendations'])

    viz_html = ""

    if credit_score_viz:
        viz_html += f"""
            <div class="card">
                <h2>Credit Score</h2>
                {credit_score_viz['gauge']}
            </div>
            <div class="card">
                <h2>Score Components</h2>
                {credit_score_viz['components']}
            </div>
        """

    if client_value_viz:
        viz_html += f"""
            <div class="card">
                <h2>Value Metrics</h2>
                {client_value_viz['radar']}
            </div>
            <div class="card">
                <h2>Total Value Score</h2>
                {client_value_viz['gauge']}
            </div>
        """

    if churn_risk_viz:
        viz_html += f"""
            <div class="card">
                <h2>Churn Risk Score</h2>
                {churn_risk_viz['gauge']}
            </div>
            <div class="card">
                <h2>Risk Components</h2>
                {churn_risk_viz['indicators']}
            </div>
        """

    if loan_rec_viz:
        viz_html += f"""
            <div class="card">
                <h2>Loan Recommendations</h2>
                {loan_rec_viz['recommendations']}
            </div>
            <div class="card">
                <h2>Likelihood to Repay</h2>
                {loan_rec_viz['gauge']}
            </div>
            <div class="card">
                <h2>Loan Application Flow</h2>
                {loan_rec_viz['funnel']}
            </div>
        """

    return viz_html


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

