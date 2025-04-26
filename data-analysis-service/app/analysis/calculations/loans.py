from sqlalchemy.orm import Session
from sqlalchemy import select, func, case, and_
from datetime import datetime

from app.analysis.calculations.credit_score import CreditScoring
from app.analysis.models import Account, Payment, Card, Loan, LoanStatus, PaymentStatus, CardType, AccountType, Installment, InstallmentStatus


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
            .outerjoin(Loan, Account.account_number == Loan.account_number) \
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
            .join(Account, Account.account_number == Loan.account_number)
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
            .outerjoin(Loan, Account.account_number == Loan.account_number)
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
