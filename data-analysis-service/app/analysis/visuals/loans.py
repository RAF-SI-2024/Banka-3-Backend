import plotly.graph_objects as go


def format_recommendations_html(recommendations):
    """Format loan recommendations into an HTML table"""
    html = """
    <div class="recommendations-section">
        <h3>Loan Recommendations</h3>
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Loan Type</th>
                    <th>Maximum Amount</th>
                    <th>Confidence</th>
                    <th>Interest Rate</th>
                    <th>Term</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
    """

    for rec in recommendations:
        confidence = rec.get('confidence', 0)
        if isinstance(confidence, float):
            confidence_pct = confidence * 100 if confidence <= 1 else confidence
        else:
            confidence_pct = 0

        amount = rec.get('max_amount', 0)
        if isinstance(amount, (int, float)):
            formatted_amount = f"${amount:,.2f}"
        else:
            formatted_amount = "$0.00"

        html += f"""
            <tr>
                <td>{rec.get('loan_type', '')}</td>
                <td>{formatted_amount}</td>
                <td>{confidence_pct:.0f}%</td>
                <td>{rec.get('interest_rate', '')}</td>
                <td>{rec.get('term', '')}</td>
                <td>{rec.get('description', '')}</td>
            </tr>
        """

    html += """
            </tbody>
        </table>
        </div>
    """
    return html


def create_loan_recommendation_visualization(recommendation_data):
    """Create visualizations for loan recommendations including funnel chart, likelihood gauge, and recommendations table"""
    try:
        # Extract data for funnel chart
        loan_history = recommendation_data.get('loan_history', {})
        if isinstance(loan_history, dict):
            funnel_data = {
                'Total Applications': loan_history.get('total_applications', 0),
                'Approved': loan_history.get('approved', 0),
                'Paid Off': loan_history.get('paid_off', 0),
                'Delinquent': loan_history.get('delinquent', 0)
            }
        else:
            # If loan_history is not a dictionary, create default funnel data
            funnel_data = {
                'Total Applications': 0,
                'Approved': 0,
                'Paid Off': 0,
                'Delinquent': 0
            }

        # Create funnel chart
        funnel = go.Figure(go.Funnel(
            y=list(funnel_data.keys()),
            x=list(funnel_data.values()),
            textinfo="value+percent initial"
        ))
        funnel.update_layout(
            title="Loan Application History",
            showlegend=False,
            width=600,
            height=400
        )

        # Create likelihood gauge
        likelihood_score = recommendation_data.get('likelihood_to_repay', 0)
        reasons = recommendation_data.get('reasons', [])

        gauge = go.Figure(go.Indicator(
            mode="gauge+number",
            value=likelihood_score,
            domain={'x': [0, 1], 'y': [0, 1]},
            title={'text': "Likelihood to Repay"},
            gauge={
                'axis': {'range': [0, 100]},
                'bar': {'color': "darkblue"},
                'steps': [
                    {'range': [0, 30], 'color': "red"},
                    {'range': [30, 70], 'color': "yellow"},
                    {'range': [70, 100], 'color': "green"}
                ]
            }
        ))
        gauge.update_layout(
            width=400,
            height=300
        )

        # Create recommendations table
        recommendations = recommendation_data.get('recommendations', [])
        recommendations_table = format_recommendations_html(recommendations)

        # Format factors
        factors = recommendation_data.get('factors', {})
        factors_html = """
        <div class="factors-section">
            <h3>Factors Considered</h3>
            <div class="factors-grid">
        """
        for factor, score in factors.items():
            # Convert score to percentage and format
            score_percent = round(score * 100, 1)
            # Create a progress bar for each factor
            factors_html += f"""
                <div class="factor-item">
                    <div class="factor-label">{factor.replace('_', ' ').title()}</div>
                    <div class="progress">
                        <div class="progress-bar" role="progressbar" style="width: {score_percent}%" 
                             aria-valuenow="{score_percent}" aria-valuemin="0" aria-valuemax="100">
                            {score_percent}%
                        </div>
                </div>
            </div>
        """
        factors_html += """
            </div>
        </div>
    """

        # Create explanation section
        explanation_html = f"""
        <div class="explanation-section">
            <h3>Analysis Details</h3>
        <div class="card">
                <div class="card-body">
                    <h4>Likelihood to Repay: {likelihood_score:.1f}%</h4>
                    <h5>Factors Considered:</h5>
                    {factors_html}
                    <h5>Reasons:</h5>
                <ul>
    """

        for reason in reasons:
            explanation_html += f"<li>{reason}</li>"

        explanation_html += """
                </ul>
                </div>
            </div>
        </div>
    """

        # Combine all components into dashboard HTML
        dashboard_html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Loan Recommendation Dashboard</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
            <script src="https://cdn.plot.ly/plotly-2.27.0.min.js"></script>
            <style>
                .dashboard-container {{
                    padding: 20px;
                    max-width: 1200px;
                    margin: 0 auto;
                }}
                .visualization-row {{
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 30px;
                }}
                .recommendations-section {{
                    margin-top: 30px;
                }}
                .explanation-section {{
                    margin-top: 30px;
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
                .table tbody + tbody {{
                    border-top: 2px solid #dee2e6;
                }}
                .table-striped tbody tr:nth-of-type(odd) {{
                    background-color: rgba(0,0,0,.05);
                }}
                .factors-section {{
                    margin-top: 20px;
                    padding: 20px;
                    background-color: #f8f9fa;
                    border-radius: 5px;
                }}
                .factors-grid {{
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                    gap: 20px;
                    margin-top: 15px;
                }}
                .factor-item {{
                    margin-bottom: 10px;
                }}
                .factor-label {{
                    margin-bottom: 5px;
                    font-weight: 500;
                }}
                .progress {{
                    height: 20px;
                    background-color: #e9ecef;
                    border-radius: 10px;
                    overflow: hidden;
                }}
                .progress-bar {{
                    background-color: #007bff;
                    color: white;
                    text-align: center;
                    line-height: 20px;
                    font-size: 12px;
                }}
            </style>
        </head>
        <body>
            <div class="dashboard-container">
                <h1>Loan Recommendation Dashboard</h1>
                <div class="visualization-row">
                    <div id="funnel"></div>
                    <div id="gauge"></div>
                </div>
                {recommendations_table}
                {explanation_html}
            </div>
            <script>
                var funnel_data = {funnel.to_json()};
                var gauge_data = {gauge.to_json()};
                Plotly.newPlot('funnel', funnel_data.data, funnel_data.layout);
                Plotly.newPlot('gauge', gauge_data.data, gauge_data.layout);
            </script>
        </body>
        </html>
        """
    except Exception as e:
        print(f"Error creating visualization: {str(e)}")
    return {
        'dashboard_html': dashboard_html,
        'funnel': funnel.to_html(full_html=False),
        'gauge': gauge.to_html(full_html=False),
        'recommendations': recommendations_table
    }
