import plotly.graph_objects as go
import plotly.express as px
import pandas as pd
from typing import Dict, Any, List


def create_credit_score_visualization(credit_score_data):
    """Create visualization for credit score analysis"""
    if not credit_score_data:
        return None

    # Create a gauge chart for the credit score
    fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=credit_score_data['credit_score'],
        domain={'x': [0.1, 0.9], 'y': [0, 0.9]},  # Adjust domain to make gauge smaller
        title={'text': "Credit Score", 'font': {'size': 24}},
        number={'font': {'size': 40}},
        gauge={
            'axis': {'range': [300, 850], 'tickwidth': 1, 'tickcolor': "darkblue"},
            'bar': {'color': "darkblue", 'thickness': 0.6},
            'steps': [
                {'range': [300, 580], 'color': "red"},
                {'range': [580, 670], 'color': "orange"},
                {'range': [670, 750], 'color': "yellow"},
                {'range': [750, 850], 'color': "green"}
            ],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': credit_score_data['credit_score']
            }
        }
    ))

    fig.update_layout(
        height=350,  # Reduce height
        margin=dict(t=50, b=0, l=25, r=25),  # Adjust margins
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Create a bar chart for score components
    components_fig = go.Figure(data=[
        go.Bar(
            x=list(credit_score_data['components'].keys()),
            y=list(credit_score_data['components'].values()),
            text=[f"{v:.2f}" for v in credit_score_data['components'].values()],
            textposition='auto',
            marker_color='rgb(82, 106, 255)'
        )
    ])
    components_fig.update_layout(
        title={
            'text': "Credit Score Components",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        xaxis_title="Component",
        yaxis_title="Score",
        yaxis_range=[0, 1],
        height=350,  # Match height with gauge
        margin=dict(t=50, b=100, l=50, r=25),
        xaxis_tickangle=-45,
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)'  # Light gray background for the plot
    )

    return {
        'gauge': fig.to_html(full_html=False, config={'displayModeBar': False}),
        'components': components_fig.to_html(full_html=False, config={'displayModeBar': False})
    }


def create_client_value_visualization(client_value_data):
    """Create visualization for client value analysis"""
    if not client_value_data:
        return None

    # Create a radar chart for value metrics
    fig = go.Figure()

    fig.add_trace(go.Scatterpolar(
        r=list(client_value_data['metrics'].values()),
        theta=list(client_value_data['metrics'].keys()),
        fill='toself',
        name='Value Metrics',
        line=dict(color='rgb(82, 106, 255)')
    ))

    fig.update_layout(
        polar=dict(
            radialaxis=dict(
                visible=True,
                range=[0, 1]
            )
        ),
        showlegend=False,
        title={
            'text': "Client Value Metrics",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Create a gauge for total score
    gauge_fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=client_value_data['total_score'],
        domain={'x': [0.1, 0.9], 'y': [0, 0.9]},
        title={'text': "Total Value Score", 'font': {'size': 24}},
        number={'font': {'size': 40}},
        gauge={
            'axis': {'range': [0, 1], 'tickwidth': 1, 'tickcolor': "darkblue"},
            'bar': {'color': "darkblue", 'thickness': 0.6},
            'steps': [
                {'range': [0, 0.4], 'color': "red"},
                {'range': [0.4, 0.8], 'color': "yellow"},
                {'range': [0.8, 1], 'color': "green"}
            ],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': client_value_data['total_score']
            }
        }
    ))
    gauge_fig.update_layout(
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    return {
        'radar': fig.to_html(full_html=False, config={'displayModeBar': False}),
        'gauge': gauge_fig.to_html(full_html=False, config={'displayModeBar': False})
    }


def create_churn_risk_visualization(churn_data):
    """Create visualization for churn risk analysis"""
    if not churn_data:
        return None

    # Create a gauge for risk score
    fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=churn_data['risk_score'],
        domain={'x': [0.1, 0.9], 'y': [0, 0.9]},
        title={'text': "Churn Risk Score", 'font': {'size': 24}},
        number={'font': {'size': 40}},
        gauge={
            'axis': {'range': [0, 1], 'tickwidth': 1, 'tickcolor': "darkblue"},
            'bar': {'color': "darkblue", 'thickness': 0.6},
            'steps': [
                {'range': [0, 0.3], 'color': "green"},
                {'range': [0.3, 0.7], 'color': "yellow"},
                {'range': [0.7, 1], 'color': "red"}
            ],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': churn_data['risk_score']
            }
        }
    ))
    fig.update_layout(
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Create a bar chart for risk indicators
    indicators_fig = go.Figure(data=[
        go.Bar(
            x=list(churn_data['indicators'].keys()),
            y=[1 if v else 0 for v in churn_data['indicators'].values()],
            text=[str(v) for v in churn_data['indicators'].values()],
            textposition='auto',
            marker_color='rgb(82, 106, 255)'
        )
    ])
    indicators_fig.update_layout(
        title={
            'text': "Risk Indicators",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        xaxis_title="Indicator",
        yaxis_title="Present",
        yaxis_range=[0, 1],
        height=350,
        margin=dict(t=50, b=100, l=50, r=25),
        xaxis_tickangle=-45,
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)'
    )

    return {
        'gauge': fig.to_html(full_html=False, config={'displayModeBar': False}),
        'indicators': indicators_fig.to_html(full_html=False, config={'displayModeBar': False})
    }


def create_product_usage_visualization(usage_data):
    """Create visualization for product usage analysis"""
    if not usage_data:
        return None

    # Create pie chart for product combinations
    combinations = usage_data['product_combinations']
    fig = go.Figure(data=[go.Pie(
        labels=list(combinations.keys()),
        values=list(combinations.values()),
        hole=.3,
        marker=dict(colors=px.colors.qualitative.Set3)
    )])
    fig.update_layout(
        title={
            'text': "Product Usage Combinations",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Create bar chart for usage stats
    stats = usage_data['usage_stats']
    stats_fig = go.Figure()
    stats_fig.add_trace(go.Bar(
        x=['Total Clients', 'Clients with Cards', 'Active Accounts'],
        y=[stats['total_clients'],
           stats['card_usage']['clients_with_cards'],
           stats['payment_activity']['active_accounts']],
        marker_color='rgb(82, 106, 255)'
    ))
    stats_fig.update_layout(
        title={
            'text': "Product Usage Statistics",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        yaxis_title="Count",
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)'
    )

    # Create loan statistics visualizations
    loan_stats = stats['loan_activity']
    
    # Total loans and amounts
    total_loans_fig = go.Figure()
    total_loans_fig.add_trace(go.Bar(
        x=['Total Loans', 'Total Amount', 'Average Amount'],
        y=[loan_stats['total_loans']['count'],
           loan_stats['total_loans']['total_amount'],
           loan_stats['total_loans']['average_amount']],
        marker_color='rgb(44, 160, 44)'
    ))
    total_loans_fig.update_layout(
        title={
            'text': "Total Loan Statistics",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        yaxis_title="Value",
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)'
    )

    # Loans by type
    loans_by_type = loan_stats['by_type']
    type_fig = go.Figure()
    for loan_type in loans_by_type:
        type_fig.add_trace(go.Bar(
            x=[loan_type['type']],
            y=[loan_type['count']],
            name=loan_type['type'],
            text=[f"${loan_type['total_amount']:,.0f}"],
            textposition='auto',
            marker_color='rgb(148, 103, 189)'
        ))
    type_fig.update_layout(
        title={
            'text': "Loans by Type",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        yaxis_title="Count",
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)',
        barmode='group'
    )

    # Loans by status
    loans_by_status = loan_stats['by_status']
    status_fig = go.Figure()
    for status in loans_by_status:
        status_fig.add_trace(go.Bar(
            x=[status['status']],
            y=[status['count']],
            name=status['status'],
            text=[f"${status['total_amount']:,.0f}"],
            textposition='auto',
            marker_color='rgb(255, 127, 14)'
        ))
    status_fig.update_layout(
        title={
            'text': "Loans by Status",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        yaxis_title="Count",
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)',
        barmode='group'
    )

    return {
        'combinations': fig.to_html(full_html=False, config={'displayModeBar': False}),
        'stats': stats_fig.to_html(full_html=False, config={'displayModeBar': False}),
        'total_loans': total_loans_fig.to_html(full_html=False, config={'displayModeBar': False}),
        'loans_by_type': type_fig.to_html(full_html=False, config={'displayModeBar': False}),
        'loans_by_status': status_fig.to_html(full_html=False, config={'displayModeBar': False})
    }


def create_client_segments_visualization(segments_data):
    """Create visualization for client segmentation analysis"""
    if not segments_data:
        return None

    # Extract data
    clusters = segments_data['clusters']
    characteristics = segments_data['segment_characteristics']
    
    # Create a DataFrame for easier plotting
    df = pd.DataFrame.from_dict(clusters, orient='index')
    
    # 1. Segment Size Distribution
    size_fig = go.Figure(data=[
        go.Pie(
            labels=[f"Segment {i}" for i in range(len(characteristics))],
            values=[char['size'] for char in characteristics.values()],
            hole=0.3,
            textinfo='label+percent',
            textposition='outside',
            marker=dict(colors=px.colors.qualitative.Pastel)
        )
    ])
    size_fig.update_layout(
        title="Client Segment Distribution",
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # 2. Balance and Activity Analysis
    balance_activity_fig = go.Figure()
    
    # Add balance bars
    balance_activity_fig.add_trace(go.Bar(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['balance'],
        name='Average Balance',
        marker_color='rgb(82, 106, 255)'
    ))
    
    # Add activity line
    balance_activity_fig.add_trace(go.Scatter(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['activity_level'],
        name='Activity Level',
        yaxis='y2',
        line=dict(color='rgb(255, 127, 14)')
    ))
    
    balance_activity_fig.update_layout(
        title="Balance and Activity by Segment",
        yaxis=dict(title="Average Balance"),
        yaxis2=dict(
            title="Activity Level",
            overlaying="y",
            side="right"
        ),
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # 3. Card Usage Analysis
    card_usage_fig = go.Figure()
    
    # Add card count bars
    card_usage_fig.add_trace(go.Bar(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['card_count'],
        name='Total Cards',
        marker_color='rgb(44, 160, 44)'
    ))
    
    # Add credit card ratio line
    card_usage_fig.add_trace(go.Scatter(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['credit_card_ratio'],
        name='Credit Card Ratio',
        yaxis='y2',
        line=dict(color='rgb(214, 39, 40)')
    ))
    
    card_usage_fig.update_layout(
        title="Card Usage by Segment",
        yaxis=dict(title="Total Cards"),
        yaxis2=dict(
            title="Credit Card Ratio",
            overlaying="y",
            side="right"
        ),
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # 4. Transaction Analysis
    transaction_fig = go.Figure()
    
    # Add transaction count bars
    transaction_fig.add_trace(go.Bar(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['transaction_count'],
        name='Transaction Count',
        marker_color='rgb(148, 103, 189)'
    ))
    
    # Add success rate line
    transaction_fig.add_trace(go.Scatter(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['transaction_success_rate'],
        name='Success Rate',
        yaxis='y2',
        line=dict(color='rgb(140, 86, 75)')
    ))
    
    transaction_fig.update_layout(
        title="Transaction Analysis by Segment",
        yaxis=dict(title="Transaction Count"),
        yaxis2=dict(
            title="Success Rate",
            overlaying="y",
            side="right"
        ),
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # 5. Segment Characteristics Table
    characteristics_df = pd.DataFrame.from_dict(characteristics, orient='index')
    characteristics_fig = go.Figure(data=[go.Table(
        header=dict(
            values=['Segment'] + list(characteristics_df.columns),
            fill_color='paleturquoise',
            align='left'
        ),
        cells=dict(
            values=[['Segment ' + str(i) for i in range(len(characteristics))]] + 
                   [characteristics_df[col] for col in characteristics_df.columns],
            fill_color='lavender',
            align='left'
        )
    )])
    characteristics_fig.update_layout(
        title="Segment Characteristics",
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # Combine all visualizations
    html_content = f"""
    <div class="container">
        <div class="card">
            {size_fig.to_html(full_html=False, include_plotlyjs=False)}
                </div>
        <div class="card">
            {balance_activity_fig.to_html(full_html=False, include_plotlyjs=False)}
            </div>
        <div class="card">
            {card_usage_fig.to_html(full_html=False, include_plotlyjs=False)}
        </div>
        <div class="card">
            {transaction_fig.to_html(full_html=False, include_plotlyjs=False)}
        </div>
        <div class="card" style="grid-column: span 2;">
            {characteristics_fig.to_html(full_html=False, include_plotlyjs=False)}
            </div>
        </div>
    """

    return html_content


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
            <script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
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

        return {
            'dashboard_html': dashboard_html,
            'funnel': funnel.to_html(full_html=False),
            'gauge': gauge.to_html(full_html=False)
        }

    except Exception as e:
        print(f"Error creating visualization: {str(e)}")
        return None


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
                <h2>Risk Indicators</h2>
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
