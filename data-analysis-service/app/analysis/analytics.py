from sqlalchemy.orm import Session
from sqlalchemy import select, func, case, and_
from datetime import datetime, timedelta
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
import pandas as pd
from .models import Account, Payment, Card, Loan, LoanStatus, PaymentStatus, CardType, AccountType, Installment, InstallmentStatus
from typing import List, Dict, Any
import numpy as np


class ClientSegmentation:
    def __init__(self, db: Session):
        self.db = db

    def get_client_features(self):
        """Extract relevant features for client segmentation based on actual data patterns"""
        # Get account information with transaction statistics
        query = select(
            Account.client_id,
            Account.balance,
            Account.type.label('account_type'),
            func.count(Payment.id).label('transaction_count'),
            func.sum(Payment.amount).label('total_transaction_amount'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.count(Card.id).label('card_count'),
            func.count(case((Card.type == CardType.CREDIT, Card.id))).label('credit_cards'),
            func.count(case((Card.type == CardType.DEBIT, Card.id))).label('debit_cards'),
            func.min(Account.creation_date).label('first_activity'),
            func.count(case((Payment.status == PaymentStatus.COMPLETED, Payment.id))).label('successful_transactions'),
            func.count(case((Account.type == AccountType.FOREIGN, Account.account_number))).label('foreign_accounts')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .group_by(Account.client_id, Account.balance, Account.type)

        result = self.db.execute(query).all()

        # Convert to DataFrame for easier processing
        df = pd.DataFrame(result, columns=[
            'client_id', 'balance', 'account_type',
            'transaction_count', 'total_transaction_amount', 'avg_transaction_amount',
            'card_count', 'credit_cards', 'debit_cards',
            'first_activity', 'successful_transactions', 'foreign_accounts'
        ])

        # Replace None values with 0 for numeric columns
        numeric_columns = ['balance', 'transaction_count', 'total_transaction_amount', 
                         'avg_transaction_amount', 'card_count', 'credit_cards',
                         'debit_cards', 'successful_transactions', 'foreign_accounts']
        df[numeric_columns] = df[numeric_columns].fillna(0)

        # Calculate additional metrics based on actual data patterns
        df['transaction_success_rate'] = df['successful_transactions'] / df['transaction_count'].replace(0, 1)
        df['credit_card_ratio'] = df['credit_cards'] / df['card_count'].replace(0, 1)
        df['days_active'] = (datetime.now() - pd.to_datetime(df['first_activity'])).dt.days
        df['activity_level'] = df['transaction_count'] / df['days_active'].replace(0, 1)
        df['has_foreign_account'] = df['foreign_accounts'] > 0
        
        # Create balance level using a simple function
        def get_balance_level(balance):
            if balance > 50000:
                return 'Very High'
            elif balance > 20000:
                return 'High'
            elif balance > 5000:
                return 'Moderate'
            elif balance > 1000:
                return 'Low'
            else:
                return 'Very Low'
        
        df['balance_level'] = df['balance'].apply(get_balance_level)

        # Replace NaN and inf values with 0
        df = df.replace([np.inf, -np.inf], np.nan)
        df = df.fillna(0)

        return df

    def perform_clustering(self, n_clusters=5):
        """Perform k-means clustering on client data with features matching actual patterns"""
        df = self.get_client_features()

        # Select features for clustering based on actual data patterns
        features = df[[
            'balance', 'transaction_count', 'total_transaction_amount',
            'avg_transaction_amount', 'card_count', 'credit_card_ratio',
            'transaction_success_rate', 'activity_level', 'has_foreign_account'
        ]]

        # Standardize features
        scaler = StandardScaler()
        features_scaled = scaler.fit_transform(features)

        # Perform k-means clustering
        kmeans = KMeans(n_clusters=n_clusters, random_state=42)
        df['cluster'] = kmeans.fit_predict(features_scaled)

        # Analyze clusters based on actual data patterns
        cluster_stats = df.groupby('cluster').agg({
            'balance': 'mean',
            'transaction_count': 'mean',
            'total_transaction_amount': 'mean',
            'avg_transaction_amount': 'mean',
            'card_count': 'mean',
            'credit_card_ratio': 'mean',
            'transaction_success_rate': 'mean',
            'activity_level': 'mean',
            'has_foreign_account': 'mean',
            'client_id': 'count'
        }).round(2)

        # Calculate segment characteristics based on actual data patterns
        segment_characteristics = {}
        for cluster in cluster_stats.index:
            stats = cluster_stats.loc[cluster]
            characteristics = {
                'size': int(stats['client_id']),
                'balance_level': 'Very High' if stats['balance'] > 50000 else 'High' if stats['balance'] > 20000 else 'Moderate' if stats['balance'] > 5000 else 'Low' if stats['balance'] > 1000 else 'Very Low',
                'activity_level': 'High' if stats['activity_level'] > 0.7 else 'Medium' if stats['activity_level'] > 0.3 else 'Low',
                'transaction_volume': 'High' if stats['total_transaction_amount'] > 0.7 else 'Medium' if stats['total_transaction_amount'] > 0.3 else 'Low',
                'card_usage': 'Multiple' if stats['card_count'] > 2 else 'Standard' if stats['card_count'] > 0 else 'None',
                'credit_card_usage': 'High' if stats['credit_card_ratio'] > 0.7 else 'Medium' if stats['credit_card_ratio'] > 0.3 else 'Low',
                'success_rate': 'High' if stats['transaction_success_rate'] > 0.9 else 'Medium' if stats['transaction_success_rate'] > 0.7 else 'Low',
                'international_activity': 'Yes' if stats['has_foreign_account'] > 0.5 else 'No'
            }
            segment_characteristics[cluster] = characteristics

        return {
            'clusters': cluster_stats.to_dict('index'),
            'client_segments': df[['client_id', 'cluster']].to_dict('records'),
            'segment_characteristics': segment_characteristics
        }


class LoanRecommendation:
    def __init__(self, db: Session):
        self.db = db

    def get_client_loan_features(self, client_id: int):
        """Get comprehensive features for loan recommendation"""
        # Get account and transaction history
        query = select(
            Account.balance,
            Account.type.label('account_type'),
            func.count(Payment.id).label('transaction_count'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.sum(case(
                (Payment.status == PaymentStatus.COMPLETED, 1),
                else_=0
            )).label('successful_transactions'),
            func.count(Card.id).label('card_count'),
            func.count(case((Card.type == CardType.CREDIT, Card.id))).label('credit_cards'),
            func.count(case((Card.type == CardType.DEBIT, Card.id))).label('debit_cards'),
            func.min(Account.creation_date).label('first_activity'),
            func.count(case((Account.type == AccountType.FOREIGN, Account.account_number))).label('foreign_accounts'),
            func.count(Loan.id).label('existing_loans'),
            func.sum(case(
                (Loan.status == LoanStatus.APPROVED, Loan.amount),
                else_=0
            )).label('active_loan_amount'),
            func.avg(Account.balance).label('avg_balance'),
            func.min(Account.balance).label('min_balance'),
            func.count(case((Payment.status == PaymentStatus.FAILED, Payment.id))).label('failed_transactions'),
            func.count(case((and_(Card.type == CardType.CREDIT, Account.balance < 0), Card.id))).label('overlimit_cards')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .outerjoin(Loan, Account.account_number == Loan.account_account_number) \
            .where(Account.client_id == client_id) \
            .group_by(Account.balance, Account.type)

        result = self.db.execute(query).first()

        if result:
            days_active = (datetime.now() - datetime.combine(result.first_activity,
                                                         datetime.min.time())).days if result.first_activity else 0
            
            # Calculate transaction success rate
            transaction_success_rate = result.successful_transactions / result.transaction_count if result.transaction_count > 0 else 0
            
            # Calculate credit card payment ratio (assuming successful transactions indicate payments)
            credit_card_payment_ratio = result.successful_transactions / result.transaction_count if result.transaction_count > 0 else 0
            
            # Calculate active days ratio (assuming at least one transaction per active day)
            active_days_ratio = result.transaction_count / days_active if days_active > 0 else 0
            
            # Calculate transaction failure rate
            transaction_failure_rate = result.failed_transactions / result.transaction_count if result.transaction_count > 0 else 0
            
            return {
                'balance': float(result.balance),
                'account_type': result.account_type,
                'transaction_count': result.transaction_count,
                'avg_transaction_amount': float(result.avg_transaction_amount) if result.avg_transaction_amount else 0,
                'transaction_success_rate': transaction_success_rate,
                'card_count': result.card_count,
                'credit_cards': result.credit_cards,
                'debit_cards': result.debit_cards,
                'days_active': days_active,
                'has_foreign_account': result.foreign_accounts > 0,
                'existing_loans': result.existing_loans,
                'active_loan_amount': float(result.active_loan_amount) if result.active_loan_amount else 0,
                'credit_card_ratio': result.credit_cards / result.card_count if result.card_count > 0 else 0,
                'activity_level': result.transaction_count / days_active if days_active > 0 else 0,
                'avg_balance': float(result.avg_balance) if result.avg_balance else 0,
                'min_balance': float(result.min_balance) if result.min_balance else 0,
                'credit_card_payment_ratio': credit_card_payment_ratio,
                'active_days_ratio': active_days_ratio,
                'transaction_failure_rate': transaction_failure_rate,
                'credit_card_overlimit_count': result.overlimit_cards,
                'overdraft_count': 0,  # This would need to be calculated from transaction history
                'savings_ratio': 0.1,  # Placeholder - would need to be calculated from account types
                'savings_consistency': 0.8,  # Placeholder - would need to be calculated from transaction patterns
                'income_consistency': 0.9,  # Placeholder - would need to be calculated from transaction patterns
                'income_growth_rate': 0.05,  # Placeholder - would need to be calculated from transaction patterns
                'loan_success_rate': 0.9,  # Placeholder - would need to be calculated from loan history
                'total_loans': result.existing_loans,
                'paid_off_loans': 0,  # Placeholder - would need to be calculated from loan history
                'delinquent_loans': 0  # Placeholder - would need to be calculated from loan history
            }
        return None

    def get_loan_history(self, client_id: int):
        """Get loan history statistics for a client"""
        # Get loan history through installments
        loan_history = self.db.execute(
            select(
                func.count(func.distinct(Loan.id)).label('total_applications'),
                func.count(func.distinct(case((Loan.status == LoanStatus.APPROVED, Loan.id)))).label('approved'),
                func.count(func.distinct(case((Loan.status == LoanStatus.PAID_OFF, Loan.id)))).label('paid_off'),
                func.count(func.distinct(case((Loan.status == LoanStatus.DELINQUENT, Loan.id)))).label('delinquent'),
                func.count(func.distinct(Installment.id)).label('total_installments'),
                func.count(func.distinct(case((Installment.installment_status == InstallmentStatus.PAID, Installment.id)))).label('paid_installments')
            )
            .join(Account, Account.account_number == Loan.account_account_number)
            .outerjoin(Installment, Loan.id == Installment.loan_id)
            .where(Account.client_id == client_id)
        ).first()

        if loan_history:
            return {
                'total_applications': loan_history.total_applications or 0,
                'approved': loan_history.approved or 0,
                'paid_off': loan_history.paid_off or 0,
                'delinquent': loan_history.delinquent or 0,
                'payment_reliability': (loan_history.paid_installments / loan_history.total_installments 
                                      if loan_history.total_installments > 0 else 0)
            }
        return None

    def calculate_likelihood_to_repay(self, client_id: int):
        """Calculate likelihood to repay score based on credit scoring"""
        # Initialize credit scoring
        credit_scoring = CreditScoring(self.db)
        
        # Get credit score
        credit_score_data = credit_scoring.calculate_credit_score(client_id)
        if not credit_score_data:
            return 0.5, ["Insufficient data for credit scoring"], {}
            
        # Convert credit score to likelihood (300-850 scale to 0.2-0.8 range)
        credit_score = credit_score_data['credit_score']
        likelihood_score = (0.2 + ((credit_score - 300) / 550) * 0.6)*100
        
        # Get the factors considered from credit score components
        factors = {
            'balance_score': credit_score_data['components'].get('balance_score', 0),
            'transaction_reliability': credit_score_data['components'].get('transaction_reliability', 0),
            'activity_score': credit_score_data['components'].get('activity_score', 0),
            'average_amount_score': credit_score_data['components'].get('average_amount_score', 0),
            'product_diversity': credit_score_data['components'].get('product_diversity', 0),
            'account_age_score': credit_score_data['components'].get('account_age_score', 0)
        }
        
        return likelihood_score, credit_score_data.get('reasons', []), factors

    def recommend_loan(self, client_id: int):
        """Recommend loan products based on client's financial behavior"""
        features = self.get_client_loan_features(client_id)
        if not features:
            return None
        
        loan_history_stats = self.get_loan_history(client_id)
        likelihood_score, reasons, factors = self.calculate_likelihood_to_repay(client_id)
        
        recommendations = []
        
        # Premium client recommendations
        if likelihood_score > 70:
            if features['balance'] > 10000:
                recommendations.append({
                    'loan_type': 'PREMIUM',
                    'max_amount': min(features['balance'] * 5, 200000),
                    'confidence': 0.9,
                    'interest_rate': 'Premium',
                    'term': '12-84 months',
                    'description': 'Premium loan with preferential terms',
                    'reasons': reasons
                })

        # Standard client recommendations
        if likelihood_score >= 60:
            recommendations.append({
                'loan_type': 'PERSONAL',
                'max_amount': min(features['balance'] * 3, 100000),
                'confidence': 0.7,
                'interest_rate': 'Standard',
                'term': '12-60 months',
                'description': 'Personal loan with standard terms',
                'reasons': reasons
            })
            if features['credit_card_ratio'] < 0.5:
                recommendations.append({
                    'loan_type': 'CREDIT_CARD',
                    'max_amount': min(features['balance'] * 0.3, 20000),
                    'confidence': 0.6,
                    'interest_rate': 'Standard',
                    'term': 'Revolving',
                    'description': 'Standard credit card with competitive rates',
                    'reasons': reasons
                })

        # Basic client recommendations
        if features['transaction_count'] > 20 and features['transaction_success_rate'] > 0.7:
            recommendations.append({
                'loan_type': 'PERSONAL',
                'max_amount': min(features['balance'] * 2, 50000),
                'confidence': 0.5,
                'interest_rate': 'Standard',
                'term': '12-36 months',
                'description': 'Basic personal loan with standard terms',
                'reasons': reasons
            })
            if features['card_count'] == 0 and features['balance'] > 1000:
                recommendations.append({
                    'loan_type': 'CREDIT_CARD',
                    'max_amount': min(features['balance'] * 0.2, 10000),
                    'confidence': 0.4,
                    'interest_rate': 'Standard',
                    'term': 'Revolving',
                    'description': 'Basic credit card with standard terms',
                    'reasons': reasons
                })

        # Add special offers based on client behavior
        if features['has_foreign_account'] and not any(r['loan_type'] == 'FOREIGN_CURRENCY' for r in recommendations):
            recommendations.append({
                'loan_type': 'FOREIGN_CURRENCY',
                'max_amount': min(features['balance'] * 2, 100000),
                'confidence': 0.7,
                'interest_rate': 'Competitive',
                'term': 'Flexible',
                'description': 'Foreign currency loan with competitive rates',
                'reasons': reasons
            })

        if features['activity_level'] > 0.5 and not any(r['loan_type'] == 'BUSINESS' for r in recommendations):
            recommendations.append({
                'loan_type': 'BUSINESS',
                'max_amount': min(features['balance'] * 3, 150000),
                'confidence': 0.6,
                'interest_rate': 'Business',
                'term': '12-60 months',
                'description': 'Business loan for active clients',
                'reasons': reasons
            })

        return {
            'client_id': client_id,
            'likelihood_to_repay': likelihood_score,
            'features': features,
            'recommendations': recommendations,
            'loan_history': loan_history_stats,
            'reasons': reasons,
            'factors': factors
        }


class LoanAnalytics:
    def __init__(self, db: Session):
        self.db = db

    def get_loan_statistics(self):
        """Get comprehensive loan statistics"""
        # Get total loan count and amount
        total_stats = self.db.execute(
            select(
                func.count(Loan.id).label("total_count"),
                func.sum(Loan.amount).label("total_amount"),
                func.avg(Loan.amount).label("avg_amount"),
                func.count(func.distinct(Installment.id)).label("total_installments"),
                func.count(func.distinct(case((Installment.installment_status == InstallmentStatus.PAID, Installment.id)))).label("paid_installments"),
                func.count(func.distinct(case((Loan.status == LoanStatus.APPROVED, Loan.id)))).label("approved_count"),
                func.count(func.distinct(case((Loan.status == LoanStatus.PAID_OFF, Loan.id)))).label("paid_off_count")
            )
            .outerjoin(Installment, Loan.id == Installment.loan_id)
        ).first()

        # Get loan count by type
        type_stats = self.db.execute(
            select(
                Loan.type,
                func.count(Loan.id).label("count"),
                func.sum(Loan.amount).label("total_amount"),
                func.avg(Loan.nominal_interest_rate).label("avg_nominal_interest_rate"),
                func.avg(Loan.repayment_period).label("avg_repayment_period"),
                func.count(func.distinct(Installment.id)).label("total_installments"),
                func.count(func.distinct(case((Installment.installment_status == InstallmentStatus.PAID, Installment.id)))).label("paid_installments"),
                func.count(func.distinct(case((Loan.status == LoanStatus.APPROVED, Loan.id)))).label("approved_count")
            )
            .outerjoin(Installment, Loan.id == Installment.loan_id)
            .group_by(Loan.type)
        ).all()

        # Get loan count by status
        status_stats = self.db.execute(
            select(
                Loan.status,
                func.count(Loan.id).label("count"),
                func.sum(Loan.amount).label("total_amount"),
                func.count(func.distinct(Installment.id)).label("total_installments"),
                func.count(func.distinct(case((Installment.installment_status == InstallmentStatus.PAID, Installment.id)))).label("paid_installments")
            )
            .outerjoin(Installment, Loan.id == Installment.loan_id)
            .group_by(Loan.status)
        ).all()

        # Get client loan statistics
        client_stats = self.db.execute(
            select(
                Account.client_id,
                func.count(Loan.id).label("loan_count"),
                func.sum(Loan.amount).label("total_loan_amount"),
                func.avg(Loan.amount).label("avg_loan_amount"),
                func.count(func.distinct(Installment.id)).label("total_installments"),
                func.count(func.distinct(case((Installment.installment_status == InstallmentStatus.PAID, Installment.id)))).label("paid_installments")
            )
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .outerjoin(Installment, Loan.id == Installment.loan_id)
            .group_by(Account.client_id)
        ).all()

        # Calculate delinquency rate based on installments
        delinquency_stats = self.db.execute(
            select(
                func.count(func.distinct(case((Installment.installment_status == InstallmentStatus.LATE, Installment.id)))).label("delinquent_count"),
                func.count(func.distinct(Installment.id)).label("total_count")
            )
            .select_from(Installment)
        ).first()

        return {
            "total_loans": {
                "count": total_stats.total_count,
                "total_amount": float(total_stats.total_amount) if total_stats.total_amount else 0,
                "average_amount": float(total_stats.avg_amount) if total_stats.avg_amount else 0,
                "total_installments": total_stats.total_installments,
                "paid_installments": total_stats.paid_installments,
                "payment_rate": total_stats.paid_installments / total_stats.total_installments if total_stats.total_installments > 0 else 0,
                "approved_count": total_stats.approved_count,
                "paid_off_count": total_stats.paid_off_count
            },
            "by_type": [
                {
                    "type": stat.type.value,
                    "count": stat.count,
                    "total_amount": float(stat.total_amount) if stat.total_amount else 0,
                    "average_interest_rate": float(stat.avg_nominal_interest_rate) if stat.avg_nominal_interest_rate else 0,
                    "average_repayment_period": float(stat.avg_repayment_period) if stat.avg_repayment_period else 0,
                    "total_installments": stat.total_installments,
                    "paid_installments": stat.paid_installments,
                    "payment_rate": stat.paid_installments / stat.total_installments if stat.total_installments > 0 else 0,
                    "approved_count": stat.approved_count
                }
                for stat in type_stats
            ],
            "by_status": [
                {
                    "status": stat.status.value,
                    "count": stat.count,
                    "total_amount": float(stat.total_amount) if stat.total_amount else 0,
                    "total_installments": stat.total_installments,
                    "paid_installments": stat.paid_installments,
                    "payment_rate": stat.paid_installments / stat.total_installments if stat.total_installments > 0 else 0
                }
                for stat in status_stats
            ],
            "client_statistics": {
                "total_clients_with_loans": len([s for s in client_stats if s.loan_count > 0]),
                "average_loans_per_client": sum(s.loan_count for s in client_stats) / len(client_stats) if client_stats else 0,
                "delinquency_rate": delinquency_stats.delinquent_count / delinquency_stats.total_count if delinquency_stats.total_count > 0 else 0
            }
        }


class ProductUsageAnalytics:
    def __init__(self, db: Session):
        self.db = db

    def get_product_usage_stats(self):
        """Get comprehensive product engagement analysis"""
        # Get total number of clients
        total_clients = self.db.execute(
            select(func.count(func.distinct(Account.client_id)))
        ).scalar()

        # Get account statistics
        account_stats = self.db.execute(
            select(
                func.count(func.distinct(Account.client_id)).label('clients_with_accounts'),
                func.count(Account.account_number).label('total_accounts'),
                func.count(case((Account.type == AccountType.FOREIGN, Account.account_number))).label('foreign_accounts'),
                func.count(case((Account.type == AccountType.CURRENT, Account.account_number))).label('current_accounts'),
                func.avg(Account.balance).label('avg_balance')
            )
        ).first()

        # Get card usage statistics
        card_stats = self.db.execute(
            select(
                func.count(func.distinct(Account.client_id)).label('clients_with_cards'),
                func.count(Card.id).label('total_cards'),
                func.count(case((Card.type == CardType.CREDIT, Card.id))).label('credit_cards'),
                func.count(case((Card.type == CardType.DEBIT, Card.id))).label('debit_cards'),
                func.avg(case((Card.type == CardType.CREDIT, Card.card_limit))).label('avg_credit_limit')
            ).outerjoin(Card, Account.account_number == Card.account_number)
        ).first()

        # Get payment statistics
        payment_stats = self.db.execute(
            select(
                func.count(func.distinct(Payment.sender_account_number)).label('active_accounts'),
                func.count(Payment.id).label('total_payments'),
                func.avg(Payment.amount).label('avg_payment_amount'),
                func.sum(case((Payment.status == PaymentStatus.COMPLETED, Payment.amount))).label('total_completed_amount'),
                func.count(case((Payment.status == PaymentStatus.FAILED, Payment.id))).label('failed_payments')
            )
        ).first()

        # Get loan statistics
        loan_analytics = LoanAnalytics(self.db)
        loan_stats = loan_analytics.get_loan_statistics()

        # Calculate product correlations
        correlation_stats = self.db.execute(
            select(
                func.count(func.distinct(case((
                    and_(
                        Account.client_id.isnot(None),
                        Card.id.isnot(None)
                    ), Account.client_id
                )))).label('accounts_with_cards'),
                func.count(func.distinct(case((
                    and_(
                        Account.client_id.isnot(None),
                        Loan.id.isnot(None)
                    ), Account.client_id
                )))).label('accounts_with_loans'),
                func.count(func.distinct(case((
                    and_(
                        Card.id.isnot(None),
                        Loan.id.isnot(None)
                    ), Account.client_id
                )))).label('cards_with_loans'),
                func.count(func.distinct(case((
                    and_(
                        Account.client_id.isnot(None),
                        Card.id.isnot(None),
                        Loan.id.isnot(None)
                    ), Account.client_id
                )))).label('all_products')
            ).outerjoin(Card, Account.account_number == Card.account_number)
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
        ).first()

        # Calculate product adoption over time
        adoption_stats = self.db.execute(
            select(
                func.date_trunc('month', Account.creation_date).label('month'),
                func.count(func.distinct(Account.client_id)).label('new_clients'),
                func.count(func.distinct(case((Card.id.isnot(None), Account.client_id)))).label('new_card_users'),
                func.count(func.distinct(case((Loan.id.isnot(None), Account.client_id)))).label('new_loan_users')
            ).outerjoin(Card, Account.account_number == Card.account_number)
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .group_by('month')
            .order_by('month')
        ).all()

        # Calculate segment-based product usage
        segment_stats = self.db.execute(
            select(
                case(
                    (Account.balance > 10000, 'Premium'),
                    (Account.balance > 5000, 'Standard'),
                    else_='Basic'
                ).label('segment'),
                func.count(func.distinct(Account.client_id)).label('client_count'),
                func.count(Card.id).label('card_count'),
                func.count(Loan.id).label('loan_count'),
                func.avg(Account.balance).label('avg_balance'),
                func.avg(case((Card.type == CardType.CREDIT, Card.card_limit))).label('avg_credit_limit')
            ).outerjoin(Card, Account.account_number == Card.account_number)
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .group_by('segment')
        ).all()

        return {
            'total_clients': total_clients,
            'account_engagement': {
                'clients_with_accounts': account_stats.clients_with_accounts,
                'total_accounts': account_stats.total_accounts,
                'foreign_accounts': account_stats.foreign_accounts,
                'current_accounts': account_stats.current_accounts,
                'avg_balance': float(account_stats.avg_balance) if account_stats.avg_balance else 0,
                'account_penetration_rate': round(account_stats.clients_with_accounts / total_clients if total_clients > 0 else 0, 2)
            },
            'card_engagement': {
                'clients_with_cards': card_stats.clients_with_cards,
                'total_cards': card_stats.total_cards,
                'credit_cards': card_stats.credit_cards,
                'debit_cards': card_stats.debit_cards,
                'avg_credit_limit': float(card_stats.avg_credit_limit) if card_stats.avg_credit_limit else 0,
                'card_penetration_rate': round(card_stats.clients_with_cards / total_clients if total_clients > 0 else 0, 2)
            },
            'payment_activity': {
                'active_accounts': payment_stats.active_accounts,
                'total_payments': payment_stats.total_payments,
                'avg_payment_amount': round(float(payment_stats.avg_payment_amount) if payment_stats.avg_payment_amount else 0, 2),
                'total_completed_amount': float(payment_stats.total_completed_amount) if payment_stats.total_completed_amount else 0,
                'failed_payments': payment_stats.failed_payments,
                'payment_success_rate': round(1 - (payment_stats.failed_payments / payment_stats.total_payments if payment_stats.total_payments > 0 else 0), 2)
            },
            'product_correlations': {
                'accounts_with_cards': correlation_stats.accounts_with_cards,
                'accounts_with_loans': correlation_stats.accounts_with_loans,
                'cards_with_loans': correlation_stats.cards_with_loans,
                'all_products': correlation_stats.all_products,
                'correlation_rates': {
                    'account_card_correlation': round(correlation_stats.accounts_with_cards / account_stats.clients_with_accounts if account_stats.clients_with_accounts > 0 else 0, 2),
                    'account_loan_correlation': round(correlation_stats.accounts_with_loans / account_stats.clients_with_accounts if account_stats.clients_with_accounts > 0 else 0, 2),
                    'card_loan_correlation': round(correlation_stats.cards_with_loans / card_stats.clients_with_cards if card_stats.clients_with_cards > 0 else 0, 2),
                    'full_product_correlation': round(correlation_stats.all_products / total_clients if total_clients > 0 else 0, 2)
                }
            },
            'product_adoption': [
                {
                    'month': str(stat.month),
                    'new_clients': stat.new_clients,
                    'new_card_users': stat.new_card_users,
                    'new_loan_users': stat.new_loan_users,
                    'card_adoption_rate': round(stat.new_card_users / stat.new_clients if stat.new_clients > 0 else 0, 2),
                    'loan_adoption_rate': round(stat.new_loan_users / stat.new_clients if stat.new_clients > 0 else 0, 2)
                }
                for stat in adoption_stats
            ],
            'segment_analysis': [
                {
                    'segment': stat.segment,
                    'client_count': stat.client_count,
                    'card_count': stat.card_count,
                    'loan_count': stat.loan_count,
                    'avg_balance': float(stat.avg_balance) if stat.avg_balance else 0,
                    'avg_credit_limit': float(stat.avg_credit_limit) if stat.avg_credit_limit else 0,
                    'cards_per_client': round(stat.card_count / stat.client_count if stat.client_count > 0 else 0, 2),
                    'loans_per_client': round(stat.loan_count / stat.client_count if stat.client_count > 0 else 0, 2)
                }
                for stat in segment_stats
            ],
            'loan_activity': loan_stats
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
            func.count(case((Payment.status == PaymentStatus.COMPLETED, Payment.id))).label('successful_transactions'),
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

    def get_client_loan_stats(self) -> List[Dict[str, Any]]:
        """Get loan statistics for each client."""
        client_stats = self.db.execute(
            select(
                Account.client_id,
                func.count(Loan.id).label("loan_count"),
                func.sum(Loan.amount).label("total_loan_amount"),
                func.avg(Loan.amount).label("avg_loan_amount")
            )
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .group_by(Account.client_id)
        ).all()

        return [
            {
                "client_id": stat.client_id,
                "loan_count": stat.loan_count or 0,
                "total_loan_amount": float(stat.total_loan_amount or 0),
                "avg_loan_amount": float(stat.avg_loan_amount or 0)
            }
            for stat in client_stats
        ]

    def get_loan_analysis(self) -> Dict[str, Any]:
        """Get comprehensive loan analysis."""
        # Total loans and amounts
        loan_stats = self.db.execute(
            select(
                func.count(Loan.id).label("total_loans"),
                func.sum(Loan.amount).label("total_amount"),
                func.avg(Loan.amount).label("avg_amount"),
                func.avg(Loan.nominal_interest_rate).label("avg_nominal_interest_rate"),
                func.avg(Loan.effective_interest_rate).label("avg_effective_interest_rate")
            )
        ).first()

        # Loans by type
        loans_by_type = self.db.execute(
            select(
                Loan.type,
                func.count(Loan.id).label("count"),
                func.sum(Loan.amount).label("total_amount"),
                func.avg(Loan.nominal_interest_rate).label("avg_nominal_interest_rate"),
                func.avg(Loan.effective_interest_rate).label("avg_effective_interest_rate"),
                func.avg(Loan.repayment_period).label("avg_repayment_period")
            )
            .group_by(Loan.type)
        ).all()

        # Loans by status
        loans_by_status = self.db.execute(
            select(
                Loan.status,
                func.count(Loan.id).label("count"),
                func.sum(Loan.amount).label("total_amount")
            )
            .group_by(Loan.status)
        ).all()

        # Average loan amount by client segment
        avg_loan_by_segment = self.db.execute(
            select(
                Account.client_id,
                func.avg(Loan.amount).label("avg_loan_amount")
            )
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .group_by(Account.client_id)
        ).all()

        return {
            "total_loans": loan_stats.total_loans or 0,
            "total_amount": float(loan_stats.total_amount or 0),
            "avg_amount": float(loan_stats.avg_amount or 0),
            "avg_nominal_interest_rate": float(loan_stats.avg_nominal_interest_rate or 0),
            "avg_effective_interest_rate": float(loan_stats.avg_effective_interest_rate or 0),
            "loans_by_type": [
                {
                    "type": loan_type.type.value,
                    "count": loan_type.count,
                    "total_amount": float(loan_type.total_amount or 0),
                    "avg_nominal_interest_rate": float(loan_type.avg_nominal_interest_rate or 0),
                    "avg_effective_interest_rate": float(loan_type.avg_effective_interest_rate or 0),
                    "avg_repayment_period": float(loan_type.avg_repayment_period or 0)
                }
                for loan_type in loans_by_type
            ],
            "loans_by_status": [
                {
                    "status": loan_status.status.value,
                    "count": loan_status.count,
                    "total_amount": float(loan_status.total_amount or 0)
                }
                for loan_status in loans_by_status
            ],
            "avg_loan_by_segment": [
                {
                    "client_id": segment.client_id,
                    "avg_loan_amount": float(segment.avg_loan_amount or 0)
                }
                for segment in avg_loan_by_segment
            ]
        }

    def get_loan_recommendations(self, client_id: int) -> Dict[str, Any]:
        """Get loan recommendations for a specific client."""
        # Get client's financial profile
        client_profile = self.db.execute(
            select(
                Account.client_id,
                func.count(Loan.id).label("existing_loans"),
                func.sum(Loan.amount).label("total_loan_amount"),
                func.avg(Loan.amount).label("avg_loan_amount"),
                func.avg(Account.balance).label("avg_balance"),
                func.count(Payment.id).label("payment_count"),
                func.avg(Payment.amount).label("avg_payment_amount")
            )
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .outerjoin(Payment, Account.account_number == Payment.sender_account_number)
            .where(Account.client_id == client_id)
            .group_by(Account.client_id)
        ).first()

        if not client_profile:
            return {"error": "Client not found"}

        # Calculate recommendation score
        recommendation_score = 0
        recommended_loan_types = []

        # Check if client qualifies for premium loans
        if (client_profile.avg_balance or 0) > 10000 and (client_profile.payment_count or 0) > 10:
            recommendation_score += 2
            recommended_loan_types.append("PREMIUM")

        # Check if client qualifies for personal loans
        if (client_profile.avg_balance or 0) > 5000 and (client_profile.payment_count or 0) > 5:
            recommendation_score += 1
            recommended_loan_types.append("PERSONAL")

        # Check if client qualifies for credit card
        if (client_profile.avg_balance or 0) > 2000 and (client_profile.payment_count or 0) > 3:
            recommendation_score += 1
            recommended_loan_types.append("CREDIT_CARD")

        return {
            "client_id": client_id,
            "recommendation_score": recommendation_score,
            "recommended_loan_types": recommended_loan_types,
            "financial_profile": {
                "existing_loans": client_profile.existing_loans or 0,
                "total_loan_amount": float(client_profile.total_loan_amount or 0),
                "avg_loan_amount": float(client_profile.avg_loan_amount or 0),
                "avg_balance": float(client_profile.avg_balance or 0),
                "payment_count": client_profile.payment_count or 0,
                "avg_payment_amount": float(client_profile.avg_payment_amount or 0)
            }
        }
