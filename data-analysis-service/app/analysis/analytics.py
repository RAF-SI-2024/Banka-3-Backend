from sqlalchemy.orm import Session
from sqlalchemy import select, func, case, and_
from datetime import datetime, timedelta
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
import pandas as pd
from .models import Account, Payment, Card


class ClientSegmentation:
    def __init__(self, db: Session):
        self.db = db

    def get_client_features(self):
        """Extract relevant features for client segmentation"""
        # Get account information with transaction statistics
        query = select(
            Account.client_id,
            Account.balance,
            func.count(Payment.id).label('transaction_count'),
            func.sum(Payment.amount).label('total_transaction_amount'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.count(Card.id).label('card_count')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .group_by(Account.client_id, Account.balance)

        result = self.db.execute(query).all()

        # Convert to DataFrame for easier processing
        df = pd.DataFrame(result, columns=[
            'client_id', 'balance', 'transaction_count',
            'total_transaction_amount', 'avg_transaction_amount', 'card_count'
        ])

        # Replace None values with 0 for numeric columns
        numeric_columns = ['balance', 'transaction_count', 'total_transaction_amount', 'avg_transaction_amount', 'card_count']
        df[numeric_columns] = df[numeric_columns].fillna(0)

        return df

    def perform_clustering(self, n_clusters=5):
        """Perform k-means clustering on client data"""
        df = self.get_client_features()

        # Select only numeric features for clustering
        features = df.drop('client_id', axis=1)

        # Standardize features
        scaler = StandardScaler()
        features_scaled = scaler.fit_transform(features)

        # Perform k-means clustering
        kmeans = KMeans(n_clusters=n_clusters, random_state=42)
        df['cluster'] = kmeans.fit_predict(features_scaled)

        # Analyze clusters
        cluster_stats = df.groupby('cluster').agg({
            'balance': 'mean',
            'transaction_count': 'mean',
            'total_transaction_amount': 'mean',
            'avg_transaction_amount': 'mean',
            'card_count': 'mean',
            'client_id': 'count'
        }).round(2)

        return {
            'clusters': cluster_stats.to_dict('index'),
            'client_segments': df[['client_id', 'cluster']].to_dict('records')
        }


class LoanRecommendation:
    def __init__(self, db: Session):
        self.db = db

    def get_client_loan_features(self, client_id: int):
        """Get features for loan recommendation"""
        # Get account and transaction history
        query = select(
            Account.balance,
            func.count(Payment.id).label('transaction_count'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.sum(case(
                (Payment.status == 'COMPLETED', 1),
                else_=0
            )).label('successful_transactions'),
            func.count(Card.id).label('card_count')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .where(Account.client_id == client_id) \
            .group_by(Account.balance)

        result = self.db.execute(query).first()

        if result:
            return {
                'balance': float(result.balance),
                'transaction_count': result.transaction_count,
                'avg_transaction_amount': float(result.avg_transaction_amount) if result.avg_transaction_amount else 0,
                'transaction_success_rate': result.successful_transactions / result.transaction_count if result.transaction_count > 0 else 0,
                'card_count': result.card_count
            }
        return None

    def recommend_loan(self, client_id: int):
        """Recommend loan products based on client's financial behavior"""
        features = self.get_client_loan_features(client_id)
        if not features:
            return None

        # Simple rule-based recommendation system
        recommendations = []

        if features['balance'] > 10000 and features['transaction_success_rate'] > 0.95:
            recommendations.append({
                'loan_type': 'PREMIUM',
                'max_amount': min(features['balance'] * 3, 100000),
                'confidence': 0.9
            })

        if features['transaction_count'] > 50 and features['transaction_success_rate'] > 0.9:
            recommendations.append({
                'loan_type': 'PERSONAL',
                'max_amount': min(features['balance'] * 2, 50000),
                'confidence': 0.8
            })

        if features['card_count'] > 0 and features['balance'] > 1000:
            recommendations.append({
                'loan_type': 'CREDIT_CARD',
                'max_amount': min(features['balance'], 10000),
                'confidence': 0.7
            })

        return {
            'client_id': client_id,
            'features': features,
            'recommendations': recommendations
        }


class ProductUsageAnalytics:
    def __init__(self, db: Session):
        self.db = db

    def get_product_usage_stats(self):
        """Get statistics about product usage"""
        # Get total number of clients
        total_clients = self.db.execute(
            select(func.count(func.distinct(Account.client_id)))
        ).scalar()

        # Get card usage statistics
        card_stats = self.db.execute(
            select(
                func.count(func.distinct(Account.client_id)).label('clients_with_cards'),
                func.count(Card.id).label('total_cards')
            ).outerjoin(Card, Account.account_number == Card.account_number)
        ).first()

        # Get payment statistics
        payment_stats = self.db.execute(
            select(
                func.count(func.distinct(Payment.sender_account_number)).label('active_accounts'),
                func.count(Payment.id).label('total_payments'),
                func.avg(Payment.amount).label('avg_payment_amount')
            )
        ).first()

        return {
            'total_clients': total_clients,
            'card_usage': {
                'clients_with_cards': card_stats.clients_with_cards,
                'cards_per_client': round(card_stats.total_cards / total_clients if total_clients > 0 else 0, 2),
                'card_penetration_rate': round(
                    card_stats.clients_with_cards / total_clients if total_clients > 0 else 0, 2)
            },
            'payment_activity': {
                'active_accounts': payment_stats.active_accounts,
                'total_payments': payment_stats.total_payments,
                'avg_payment_amount': round(
                    float(payment_stats.avg_payment_amount) if payment_stats.avg_payment_amount else 0, 2)
            }
        }

    def get_product_combinations(self):
        """Analyze which products are commonly used together"""
        # Get clients with their products
        query = select(
            Account.client_id,
            func.count(func.distinct(Card.id)).label('has_card'),
            func.count(func.distinct(case(
                (Payment.amount > 1000, Payment.id),
                else_=None
            ))).label('has_large_transactions')
        ).outerjoin(Card, Account.account_number == Card.account_number) \
            .outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .group_by(Account.client_id)

        result = self.db.execute(query).all()

        # Convert to DataFrame for analysis
        df = pd.DataFrame(result, columns=['client_id', 'has_card', 'has_large_transactions'])
        df['has_card'] = df['has_card'] > 0
        df['has_large_transactions'] = df['has_large_transactions'] > 0

        # Calculate product combinations
        combinations = {
            'card_and_large_transactions': int(df[(df['has_card']) & (df['has_large_transactions'])].shape[0]),
            'only_card': int(df[(df['has_card']) & (~df['has_large_transactions'])].shape[0]),
            'only_large_transactions': int(df[(~df['has_card']) & (df['has_large_transactions'])].shape[0]),
            'no_products': int(df[(~df['has_card']) & (~df['has_large_transactions'])].shape[0])
        }

        return combinations


class ClientValueAnalysis:
    def __init__(self, db: Session):
        self.db = db

    def calculate_client_value(self, client_id: int):
        """Calculate the lifetime value of a client"""
        # Get account and transaction history
        query = select(
            Account.balance,
            func.count(Payment.id).label('transaction_count'),
            func.sum(Payment.amount).label('total_transaction_amount'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.count(Card.id).label('card_count'),
            func.min(Account.creation_date).label('first_activity')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .where(Account.client_id == client_id) \
            .group_by(Account.balance)

        result = self.db.execute(query).first()

        if not result:
            return None

        days_active = (datetime.now() - datetime.combine(result.first_activity,
                                                         datetime.min.time())).days if result.first_activity else 0

        # Calculate various value metrics
        value_metrics = {
            'balance_score': min(float(result.balance) / 10000, 1.0) if result.balance else 0,
            'transaction_score': min(result.transaction_count / 100, 1.0),
            'amount_score': min(float(result.total_transaction_amount) / 100000,
                                1.0) if result.total_transaction_amount else 0,
            'product_score': min(result.card_count / 2, 1.0),
            'loyalty_score': min(days_active / 365, 1.0)
        }

        # Calculate weighted average for final score
        weights = {
            'balance_score': 0.3,
            'transaction_score': 0.2,
            'amount_score': 0.2,
            'product_score': 0.15,
            'loyalty_score': 0.15
        }

        total_score = sum(score * weights[metric] for metric, score in value_metrics.items())

        return {
            'client_id': client_id,
            'total_score': round(total_score, 2),
            'metrics': {k: round(v, 2) for k, v in value_metrics.items()},
            'segment': 'Premium' if total_score > 0.8 else 'Standard' if total_score > 0.4 else 'Basic'
        }


class ChurnPrediction:
    def __init__(self, db: Session):
        self.db = db

    def get_churn_indicators(self, client_id: int):
        """Calculate indicators that might suggest client churn"""
        # Get recent account activity
        thirty_days_ago = datetime.now() - timedelta(days=30)
        ninety_days_ago = datetime.now() - timedelta(days=90)

        # Recent activity query
        query = select(
            Account.balance,
            func.count(case((Payment.date >= thirty_days_ago, Payment.id))).label('recent_transactions'),
            func.count(case((and_(Payment.date >= ninety_days_ago, Payment.date < thirty_days_ago), Payment.id))).label(
                'previous_transactions'),
            func.avg(case((Payment.date >= thirty_days_ago, Payment.amount))).label('recent_avg_amount'),
            func.avg(
                case((and_(Payment.date >= ninety_days_ago, Payment.date < thirty_days_ago), Payment.amount))).label(
                'previous_avg_amount')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .where(Account.client_id == client_id) \
            .group_by(Account.balance)

        result = self.db.execute(query).first()

        if not result:
            return None

        # Calculate churn risk indicators
        indicators = {
            'activity_decline': (
                                            result.previous_transactions / 2) > result.recent_transactions if result.previous_transactions > 0 else False,
            'amount_decline': (result.previous_avg_amount or 0) > (
                        result.recent_avg_amount or 0) * 1.5 if result.previous_avg_amount else False,
            'low_balance': float(result.balance) < 100 if result.balance else True,
            'inactive_recent': result.recent_transactions == 0
        }

        # Calculate risk score
        risk_weights = {
            'activity_decline': 0.3,
            'amount_decline': 0.2,
            'low_balance': 0.2,
            'inactive_recent': 0.3
        }

        risk_score = sum(risk_weights[indicator] for indicator, is_risky in indicators.items() if is_risky)

        return {
            'client_id': client_id,
            'risk_score': round(risk_score, 2),
            'risk_level': 'High' if risk_score > 0.7 else 'Medium' if risk_score > 0.3 else 'Low',
            'indicators': indicators
        }


class CreditScoring:
    def __init__(self, db: Session):
        self.db = db

    def calculate_credit_score(self, client_id: int):
        """Calculate an internal credit score for a client"""
        # Get comprehensive client history
        query = select(
            Account.balance,
            Account.creation_date,
            func.count(Payment.id).label('total_transactions'),
            func.count(case((Payment.status == 'COMPLETED', Payment.id))).label('successful_transactions'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.count(Card.id).label('card_count')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .where(Account.client_id == client_id) \
            .group_by(Account.balance, Account.creation_date)

        result = self.db.execute(query).first()

        if not result:
            return None

        # Calculate various scoring components
        days_active = (datetime.now() - datetime.combine(result.creation_date,
                                                         datetime.min.time())).days if result.creation_date else 0

        score_components = {
            'balance_score': min(float(result.balance) / 10000, 1.0) if result.balance else 0,
            'transaction_reliability': result.successful_transactions / result.total_transactions if result.total_transactions > 0 else 0,
            'activity_score': min(result.total_transactions / 100, 1.0),
            'average_amount_score': min(float(result.avg_transaction_amount) / 1000,
                                        1.0) if result.avg_transaction_amount else 0,
            'product_diversity': min(result.card_count / 2, 1.0),
            'account_age_score': min(days_active / 365, 1.0)
        }

        # Calculate weighted score
        weights = {
            'balance_score': 0.25,
            'transaction_reliability': 0.2,
            'activity_score': 0.15,
            'average_amount_score': 0.15,
            'product_diversity': 0.1,
            'account_age_score': 0.15
        }

        total_score = sum(score * weights[component] for component, score in score_components.items())

        # Convert to 300-850 scale (typical credit score range)
        credit_score = int(300 + (total_score * 550))

        return {
            'client_id': client_id,
            'credit_score': credit_score,
            'components': {k: round(v, 2) for k, v in score_components.items()},
            'rating': 'Excellent' if credit_score >= 750
            else 'Good' if credit_score >= 670
            else 'Fair' if credit_score >= 580
            else 'Poor'
        }
