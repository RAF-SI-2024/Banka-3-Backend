from sqlalchemy.orm import Session
from sqlalchemy import select, func, case, and_
from datetime import datetime, timedelta
from ..models import Account, Payment, Card, Loan, LoanStatus, PaymentStatus, CardType, AccountType, Installment, InstallmentStatus


class ChurnPrediction:
    def __init__(self, db: Session):
        self.db = db

    def get_churn_indicators(self, client_id: int):
        """Calculate indicators that might suggest client churn based on comprehensive client behavior"""
        # Get recent account activity with expanded metrics
        current_date = datetime.now().date()  # Convert to date for consistent comparison
        thirty_days_ago = current_date - timedelta(days=30)
        ninety_days_ago = current_date - timedelta(days=90)
        one_year_ago = current_date - timedelta(days=365)

        # Comprehensive activity query
        query = select(
            Account.balance,
            Account.creation_date,
            func.count(case((Payment.date >= thirty_days_ago, Payment.id))).label('recent_transactions'),
            func.count(case((and_(Payment.date >= ninety_days_ago, Payment.date < thirty_days_ago), Payment.id))).label(
                'previous_transactions'),
            func.count(Card.id).label('card_count'),
            func.count(case((Card.type == CardType.CREDIT, Card.id))).label('credit_cards'),
            func.count(Loan.id).label('total_loans'),
            func.count(case((Loan.status == LoanStatus.APPROVED, Loan.id))).label('active_loans'),
            func.count(case(
                (and_(Payment.date >= one_year_ago, Payment.status == PaymentStatus.COMPLETED), Payment.id))).label(
                'yearly_successful_transactions'),
            func.count(case((Account.type == AccountType.FOREIGN, Account.account_number))).label('foreign_accounts'),
            func.count(case((Loan.status == LoanStatus.DELINQUENT, Loan.id))).label('delinquent_loans'),
            func.count(case((Installment.installment_status == InstallmentStatus.LATE, Installment.id))).label(
                'late_installments'),
            func.count(func.distinct(Account.account_number)).label('total_accounts')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .outerjoin(Loan, Account.account_number == Loan.account_account_number) \
            .outerjoin(Installment, Loan.id == Installment.loan_id) \
            .where(Account.client_id == client_id) \
            .group_by(Account.balance, Account.creation_date)

        result = self.db.execute(query).first()

        if not result:
            return None

        # Calculate comprehensive risk indicators
        days_since_creation = (current_date - result.creation_date.date() if isinstance(result.creation_date,
                                                                                        datetime) else current_date - result.creation_date).days if result.creation_date else 0
        avg_monthly_transactions = result.yearly_successful_transactions / 12 if result.yearly_successful_transactions else 0

        # Calculate balance risk score
        balance = float(result.balance) if result.balance else 0
        balance_risk = 1.0 if balance < 100 else 0.7 if balance < 1000 else 0.4 if balance < 5000 else 0.1

        # Calculate product engagement score (inverse of engagement is risk)
        product_count = result.card_count + result.active_loans + result.foreign_accounts
        product_engagement = 1.0 - min(product_count / 3, 1.0)

        # Calculate payment issues risk
        payment_issues = min((result.late_installments + result.delinquent_loans) / max(result.total_loans, 1), 1.0)

        # Calculate activity level risk (compared to average)
        activity_risk = 0
        if avg_monthly_transactions > 0:
            current_monthly = (result.recent_transactions + result.previous_transactions) / 6  # Last 6 months
            activity_ratio = current_monthly / avg_monthly_transactions
            activity_risk = 1.0 if activity_ratio < 0.3 else 0.7 if activity_ratio < 0.5 else 0.4 if activity_ratio < 0.7 else 0.1

        # Calculate engagement trend risk
        engagement_risk = 0.8 if result.recent_transactions < 5 and days_since_creation > 180 else 0.0

        # Combine all risk factors with redistributed weights
        risk_components = {
            'balance_risk': balance_risk * 0.25,  # Balance-related risk (increased from 0.15)
            'product_engagement': product_engagement * 0.20,  # Product engagement risk (increased from 0.10)
            'payment_issues': payment_issues * 0.25,  # Payment issues (increased from 0.20)
            'activity_risk': activity_risk * 0.15,  # Current activity level risk (increased from 0.10)
            'engagement_risk': engagement_risk * 0.15  # Long-term engagement risk (unchanged)
        }

        # Calculate base risk score
        base_risk_score = sum(risk_components.values())

        # Dampen risk score for new accounts (less than 3 months old)
        if days_since_creation < 90:
            risk_score = base_risk_score * (days_since_creation / 90)
        else:
            risk_score = base_risk_score

        # Cap risk score at 1.0
        risk_score = min(risk_score, 1.0)

        return {
            'client_id': client_id,
            'risk_score': round(risk_score, 2),
            'risk_level': 'High' if risk_score > 0.7 else 'Medium' if risk_score > 0.3 else 'Low',
            'risk_components': {k: round(v, 2) for k, v in risk_components.items()},
            'metrics': {
                'days_since_creation': days_since_creation,
                'recent_transactions': result.recent_transactions,
                'previous_transactions': result.previous_transactions,
                'avg_monthly_transactions': round(avg_monthly_transactions, 2),
                'total_accounts': result.total_accounts,
                'card_count': result.card_count,
                'credit_cards': result.credit_cards,
                'total_loans': result.total_loans,
                'active_loans': result.active_loans,
                'foreign_accounts': result.foreign_accounts,
                'late_installments': result.late_installments,
                'balance': float(result.balance) if result.balance else 0
            }
        }
