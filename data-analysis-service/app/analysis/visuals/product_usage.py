import plotly.graph_objects as go
import pandas as pd


def create_product_usage_visualization(result):
    """Create visualization for product engagement analysis"""
    try:
        # Extract data
        stats = result.get('usage_stats', {})
        combinations = result.get('product_combinations', {})

        print("Creating product usage visualization")
        print("Combinations data:", combinations)

        # Create account engagement combinations chart
        if not combinations:
            print("Warning: No combinations data available")
            combinations_html = "<p>No account activity data available</p>"
        else:
            combinations_fig = go.Figure(data=[go.Pie(
                labels=[
                    'Multiple Accounts & Large Trans',
                    'Foreign & Large Trans',
                    'Only Multiple Accounts',
                    'Single Account Only'
                ],
                values=[
                    combinations.get('multiple_accounts_and_large_trans', 0),
                    combinations.get('foreign_and_large_trans', 0),
                    combinations.get('only_multiple_accounts', 0),
                    combinations.get('only_single_account', 0)
                ],
                hole=0.3,
                textinfo='label+percent',
                insidetextorientation='radial',
                marker=dict(colors=['rgb(82, 106, 255)', 'rgb(55, 83, 109)',
                                    'rgb(26, 118, 255)', 'rgb(166, 189, 255)'])
            )])

            combinations_fig.update_layout(
                title={
                    'text': 'Account Activity Patterns',
                    'y': 0.95,
                    'font': {'size': 24}
                },
                showlegend=True,
                height=450,
                margin=dict(t=80, b=20, l=20, r=20),
                legend=dict(
                    orientation="h",
                    yanchor="bottom",
                    y=1.15,
                    xanchor="left",
                    x=0.5,
                    bgcolor='rgba(255, 255, 255, 0.8)'
                )
            )

            combinations_html = combinations_fig.to_html(full_html=False, config={'displayModeBar': False})

        # Create bar chart for usage stats
        stats_fig = go.Figure()
        stats_fig.add_trace(go.Bar(
            x=['Total Clients', 'Clients with Cards', 'Active Accounts'],
            y=[stats['total_clients'],
               stats['card_engagement']['clients_with_cards'],
               stats['payment_activity']['active_accounts']],
            marker_color='rgb(82, 106, 255)'
        ))
        stats_fig.update_layout(
            title={
                'text': "Product Engagement Statistics",
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

        stats_html = stats_fig.to_html(full_html=False, config={'displayModeBar': False})

        # Create correlation heatmap
        correlation_data = stats['product_correlations']['correlation_rates']
        heatmap = go.Figure(data=go.Heatmap(
            z=[
                [1, correlation_data['account_card_correlation'], correlation_data['account_loan_correlation']],
                [correlation_data['account_card_correlation'], 1, correlation_data['card_loan_correlation']],
                [correlation_data['account_loan_correlation'], correlation_data['card_loan_correlation'], 1]
            ],
            x=['Accounts', 'Cards', 'Loans'],
            y=['Accounts', 'Cards', 'Loans'],
            colorscale='Viridis',
            showscale=True
        ))
        heatmap.update_layout(
            title={
                'text': "Product Correlation Matrix",
                'y': 0.95,
                'x': 0.5,
                'xanchor': 'center',
                'yanchor': 'top',
                'font': {'size': 24}
            },
            height=350,
            margin=dict(t=50, b=50, l=50, r=25),
            paper_bgcolor='white',
            plot_bgcolor='rgb(250, 250, 250)'
        )

        heatmap_html = heatmap.to_html(full_html=False, config={'displayModeBar': False})

        # Create adoption trends chart
        adoption_data = stats['product_adoption']
        df_adoption = pd.DataFrame(adoption_data)
        adoption_fig = go.Figure()
        adoption_fig.add_trace(go.Scatter(
            x=df_adoption['month'],
            y=df_adoption['new_clients'],
            name='New Clients',
            line=dict(color='blue')
        ))
        adoption_fig.add_trace(go.Scatter(
            x=df_adoption['month'],
            y=df_adoption['new_card_users'],
            name='New Card Users',
            line=dict(color='green')
        ))
        adoption_fig.add_trace(go.Scatter(
            x=df_adoption['month'],
            y=df_adoption['new_loan_users'],
            name='New Loan Users',
            line=dict(color='red')
        ))
        adoption_fig.update_layout(
            title={
                'text': "Product Adoption Trends",
                'y': 0.95,
                'x': 0.5,
                'xanchor': 'center',
                'yanchor': 'top',
                'font': {'size': 24}
            },
            xaxis_title="Month",
            yaxis_title="Number of Users",
            height=350,
            margin=dict(t=50, b=50, l=50, r=25),
            paper_bgcolor='white',
            plot_bgcolor='rgb(250, 250, 250)'
        )

        adoption_html = adoption_fig.to_html(full_html=False, config={'displayModeBar': False})

        # Create segment analysis chart
        segment_data = stats['segment_analysis']
        df_segment = pd.DataFrame(segment_data)
        segment_fig = go.Figure(data=[
            go.Bar(
                name='Cards per Client',
                x=df_segment['segment'],
                y=df_segment['cards_per_client'],
                marker_color='rgb(55, 83, 109)'
            ),
            go.Bar(
                name='Loans per Client',
                x=df_segment['segment'],
                y=df_segment['loans_per_client'],
                marker_color='rgb(26, 118, 255)'
            )
        ])
        segment_fig.update_layout(
            title={
                'text': "Product Usage by Client Segment",
                'y': 0.95,
                'x': 0.5,
                'xanchor': 'center',
                'yanchor': 'top',
                'font': {'size': 24}
            },
            xaxis_title="Segment",
            yaxis_title="Products per Client",
            barmode='group',
            height=350,
            margin=dict(t=50, b=50, l=50, r=25),
            paper_bgcolor='white',
            plot_bgcolor='rgb(250, 250, 250)'
        )

        segment_html = segment_fig.to_html(full_html=False, config={'displayModeBar': False})

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

        total_loans_html = total_loans_fig.to_html(full_html=False, config={'displayModeBar': False})

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

        loans_by_type_html = type_fig.to_html(full_html=False, config={'displayModeBar': False})

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

        loans_by_status_html = status_fig.to_html(full_html=False, config={'displayModeBar': False})

        return {
            'combinations': combinations_html,
            'stats': stats_html,
            'heatmap': heatmap_html,
            'adoption': adoption_html,
            'segment': segment_html,
            'total_loans': total_loans_html,
            'loans_by_type': loans_by_type_html,
            'loans_by_status': loans_by_status_html
        }
    except Exception as e:
        print(f"Error creating product usage visualization: {str(e)}")
        return None


def create_product_engagement_visualization(engagement_data):
    """Create comprehensive visualizations for product engagement analysis"""
    try:
        # 1. Account Type Analysis
        account_data = engagement_data['account_engagement']
        account_types_fig = go.Figure(data=[
            go.Bar(
                x=['Current', 'Foreign'],
                y=[account_data['current_accounts'], account_data['foreign_accounts']],
                marker_color=['rgb(26, 118, 255)', 'rgb(55, 83, 109)']
            )
        ])
        account_types_fig.update_layout(
            title='Account Types Distribution',
            xaxis_title='Account Type',
            yaxis_title='Number of Accounts',
            height=400
        )

        # 2. Card Usage Analysis
        card_data = engagement_data['card_engagement']
        card_types_fig = go.Figure(data=[
            go.Bar(
                x=['Credit Cards', 'Debit Cards'],
                y=[card_data['credit_cards'], card_data['debit_cards']],
                marker_color=['rgb(255, 127, 14)', 'rgb(44, 160, 44)']
            )
        ])
        card_types_fig.update_layout(
            title='Card Types Distribution',
            xaxis_title='Card Type',
            yaxis_title='Number of Cards',
            height=400
        )

        # 3. Correlation Heatmap
        correlation_data = engagement_data['product_correlations']['correlation_rates']
        heatmap = go.Figure(data=go.Heatmap(
            z=[
                [1, correlation_data['account_card_correlation'], correlation_data['account_loan_correlation']],
                [correlation_data['account_card_correlation'], 1, correlation_data['card_loan_correlation']],
                [correlation_data['account_loan_correlation'], correlation_data['card_loan_correlation'], 1]
            ],
            x=['Accounts', 'Cards', 'Loans'],
            y=['Accounts', 'Cards', 'Loans'],
            colorscale='Viridis',
            showscale=True
        ))
        heatmap.update_layout(
            title='Product Correlation Matrix',
            height=400
        )

        # 4. Product Adoption Trends
        adoption_data = engagement_data['product_adoption']
        df_adoption = pd.DataFrame(adoption_data)
        adoption_fig = go.Figure()
        adoption_fig.add_trace(go.Scatter(
            x=df_adoption['month'],
            y=df_adoption['new_clients'],
            name='New Clients',
            line=dict(color='blue')
        ))
        adoption_fig.add_trace(go.Scatter(
            x=df_adoption['month'],
            y=df_adoption['new_card_users'],
            name='New Card Users',
            line=dict(color='green')
        ))
        adoption_fig.add_trace(go.Scatter(
            x=df_adoption['month'],
            y=df_adoption['new_loan_users'],
            name='New Loan Users',
            line=dict(color='red')
        ))
        adoption_fig.update_layout(
            title='Product Adoption Trends',
            xaxis_title='Month',
            yaxis_title='Number of Users',
            height=400
        )

        # 5. Segment Analysis
        segment_data = engagement_data['segment_analysis']
        df_segment = pd.DataFrame(segment_data)

        # Stacked bar chart for product usage by segment
        segment_fig = go.Figure(data=[
            go.Bar(
                name='Cards per Client',
                x=df_segment['segment'],
                y=df_segment['cards_per_client'],
                marker_color='rgb(55, 83, 109)'
            ),
            go.Bar(
                name='Loans per Client',
                x=df_segment['segment'],
                y=df_segment['loans_per_client'],
                marker_color='rgb(26, 118, 255)'
            )
        ])
        segment_fig.update_layout(
            title='Product Usage by Client Segment',
            xaxis_title='Segment',
            yaxis_title='Products per Client',
            barmode='group',
            height=400
        )

        # 6. Engagement Metrics
        engagement_metrics = {
            'Account Penetration': engagement_data['account_engagement']['account_penetration_rate'],
            'Card Penetration': engagement_data['card_engagement']['card_penetration_rate'],
            'Payment Success': engagement_data['payment_activity']['payment_success_rate'],
            'Full Product Usage': engagement_data['product_correlations']['correlation_rates'][
                'full_product_correlation']
        }

        metrics_fig = go.Figure(go.Bar(
            x=list(engagement_metrics.keys()),
            y=list(engagement_metrics.values()),
            marker_color='rgb(158, 202, 225)'
        ))
        metrics_fig.update_layout(
            title='Key Engagement Metrics',
            xaxis_title='Metric',
            yaxis_title='Rate',
            height=400
        )

        # Create dashboard HTML
        dashboard_html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Product Engagement Analysis Dashboard</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
            <script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
            <style>
                .dashboard-container {{
                    padding: 20px;
                    max-width: 1200px;
                    margin: 0 auto;
                }}
                .chart-container {{
                    background-color: white;
                    padding: 15px;
                    border-radius: 5px;
                    margin-bottom: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }}
                .metrics-grid {{
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 20px;
                    margin-bottom: 20px;
                }}
                .metric-card {{
                    background-color: white;
                    padding: 15px;
                    border-radius: 5px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    text-align: center;
                }}
                .metric-value {{
                    font-size: 24px;
                    font-weight: bold;
                    color: #007bff;
                }}
                .metric-label {{
                    font-size: 14px;
                    color: #6c757d;
                }}
            </style>
        </head>
        <body>
            <div class="dashboard-container">
                <h1>Product Engagement Analysis</h1>

                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-value">{engagement_data['total_clients']}</div>
                        <div class="metric-label">Total Clients</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">{engagement_data['account_engagement']['clients_with_accounts']}</div>
                        <div class="metric-label">Active Accounts</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">{engagement_data['card_engagement']['clients_with_cards']}</div>
                        <div class="metric-label">Card Users</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">{engagement_data['product_correlations']['accounts_with_loans']}</div>
                        <div class="metric-label">Loan Users</div>
                    </div>
                </div>

                <div class="row">
                    <div class="col-md-6">
                        <div class="chart-container">
                            {account_types_fig.to_html(full_html=False, include_plotlyjs=False)}
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="chart-container">
                            {card_types_fig.to_html(full_html=False, include_plotlyjs=False)}
                        </div>
                    </div>
                </div>

                <div class="row">
                    <div class="col-md-6">
                        <div class="chart-container">
                            {heatmap.to_html(full_html=False, include_plotlyjs=False)}
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="chart-container">
                            {metrics_fig.to_html(full_html=False, include_plotlyjs=False)}
                        </div>
                    </div>
                </div>

                <div class="row">
                    <div class="col-md-6">
                        <div class="chart-container">
                            {segment_fig.to_html(full_html=False, include_plotlyjs=False)}
                        </div>
                    </div>
                    <div class="col-12">
                        <div class="chart-container">
                            {adoption_fig.to_html(full_html=False, include_plotlyjs=False)}
                        </div>
                    </div>
                </div>
            </div>
            <script>
                var account_types_data = {account_types_fig.to_json()};
                var card_types_data = {card_types_fig.to_json()};
                var heatmap_data = {heatmap.to_json()};
                var adoption_data = {adoption_fig.to_json()};
                var segment_data = {segment_fig.to_json()};
                var metrics_data = {metrics_fig.to_json()};

                Plotly.newPlot('account_types', account_types_data.data, account_types_data.layout);
                Plotly.newPlot('card_types', card_types_data.data, card_types_data.layout);
                Plotly.newPlot('heatmap', heatmap_data.data, heatmap_data.layout);
                Plotly.newPlot('adoption', adoption_data.data, adoption_data.layout);
                Plotly.newPlot('segment', segment_data.data, segment_data.layout);
                Plotly.newPlot('metrics', metrics_data.data, metrics_data.layout);
            </script>
        </body>
        </html>
        """

        return {
            'dashboard_html': dashboard_html,
            'account_types': account_types_fig.to_html(full_html=False),
            'card_types': card_types_fig.to_html(full_html=False),
            'heatmap': heatmap.to_html(full_html=False),
            'adoption': adoption_fig.to_html(full_html=False),
            'segment': segment_fig.to_html(full_html=False),
            'metrics': metrics_fig.to_html(full_html=False)
        }

    except Exception as e:
        print(f"Error creating visualization: {str(e)}")
        return None