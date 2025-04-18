from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from sqlalchemy import select, func, desc, or_
from .models import Payment, Account, PersonalAccount, CompanyAccount
from .database import get_db
from datetime import datetime, timedelta

router = APIRouter(prefix="/analysis", tags=["analysis"])


@router.get("/payments")
def get_payments(limit: int = 10, db: Session = Depends(get_db)):
    """Get recent payments"""
    try:
        result = db.execute(
            select(Payment)
            .order_by(Payment.date.desc())
            .limit(limit)
        )
        payments = result.scalars().all()
        return {
            "count": len(payments),
            "payments": [
                {
                    "id": p.id,
                    "payment_id": p.payment_id,
                    "account_number": p.account_number,
                    "amount": p.amount,
                    "currency_code": p.currency_code,
                    "type": p.payment_type,
                    "status": p.status,
                    "date": p.date,
                    "description": p.description,
                    "metadata": p.payment_metadata
                }
                for p in payments
            ]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/accounts")
def get_accounts(limit: int = 10, db: Session = Depends(get_db)):
    """Get recent accounts"""
    try:
        # Query both personal and company accounts
        result = db.execute(
            select(Account)
            .where(or_(
                Account.account_owner_type == 'PERSONAL',
                Account.account_owner_type == 'COMPANY'
            ))
            .order_by(Account.creation_date.desc())
            .limit(limit)
        )
        accounts = result.scalars().all()
        return {
            "count": len(accounts),
            "accounts": [
                {
                    "account_number": a.account_number,
                    "name": a.name,
                    "client_id": a.client_id,
                    "type": a.type.value if a.type else None,
                    "status": a.status.value if a.status else None,
                    "balance": float(a.balance) if a.balance else 0,
                    "currency_code": a.currency_code,
                    "account_owner_type": a.account_owner_type.value,
                    "company_id": a.company_id if isinstance(a, CompanyAccount) else None
                }
                for a in accounts
            ]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/payment-stats")
def get_payment_stats(db: Session = Depends(get_db)):
    """Get payment statistics"""
    try:
        # Get total payment count and amount
        total_stats = db.execute(
            select(
                func.count(Payment.id).label("total_count"),
                func.sum(Payment.amount).label("total_amount")
            )
        ).first()

        # Get payment count by type
        type_stats = db.execute(
            select(
                Payment.payment_type,
                func.count(Payment.id).label("count"),
                func.sum(Payment.amount).label("amount")
            )
            .group_by(Payment.payment_type)
        ).all()

        # Get payment count by status
        status_stats = db.execute(
            select(
                Payment.status,
                func.count(Payment.id).label("count")
            )
            .group_by(Payment.status)
        ).all()

        # Get recent payment trends (last 7 days)
        seven_days_ago = datetime.utcnow() - timedelta(days=7)
        daily_stats = db.execute(
            select(
                func.date(Payment.date).label("date"),
                func.count(Payment.id).label("count"),
                func.sum(Payment.amount).label("amount")
            )
            .where(Payment.date >= seven_days_ago)
            .group_by(func.date(Payment.date))
            .order_by(desc("date"))
        ).all()

        return {
            "total_payments": {
                "count": total_stats.total_count,
                "total_amount": float(total_stats.total_amount) if total_stats.total_amount else 0
            },
            "by_type": [
                {
                    "type": stat.payment_type,
                    "count": stat.count,
                    "amount": float(stat.amount) if stat.amount else 0
                }
                for stat in type_stats
            ],
            "by_status": [
                {
                    "status": stat.status,
                    "count": stat.count
                }
                for stat in status_stats
            ],
            "daily_trends": [
                {
                    "date": stat.date.isoformat(),
                    "count": stat.count,
                    "amount": float(stat.amount) if stat.amount else 0
                }
                for stat in daily_stats
            ]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/account-stats")
def get_account_stats(db: Session = Depends(get_db)):
    """Get account statistics"""
    try:
        # Get total account count and balance
        total_stats = db.execute(
            select(
                func.count(Account.account_number).label("total_count"),
                func.sum(Account.balance).label("total_balance")
            )
            .where(or_(
                Account.account_owner_type == 'PERSONAL',
                Account.account_owner_type == 'COMPANY'
            ))
        ).first()

        # Get account count by type
        type_stats = db.execute(
            select(
                Account.type,
                func.count(Account.account_number).label("count"),
                func.sum(Account.balance).label("total_balance")
            )
            .where(or_(
                Account.account_owner_type == 'PERSONAL',
                Account.account_owner_type == 'COMPANY'
            ))
            .group_by(Account.type)
        ).all()

        # Get account count by owner type
        owner_type_stats = db.execute(
            select(
                Account.account_owner_type,
                func.count(Account.account_number).label("count"),
                func.sum(Account.balance).label("total_balance")
            )
            .group_by(Account.account_owner_type)
        ).all()

        # Get account count by status
        status_stats = db.execute(
            select(
                Account.status,
                func.count(Account.account_number).label("count")
            )
            .where(or_(
                Account.account_owner_type == 'PERSONAL',
                Account.account_owner_type == 'COMPANY'
            ))
            .group_by(Account.status)
        ).all()

        # Get top accounts by balance
        top_accounts = db.execute(
            select(Account)
            .where(or_(
                Account.account_owner_type == 'PERSONAL',
                Account.account_owner_type == 'COMPANY'
            ))
            .order_by(Account.balance.desc())
            .limit(10)
        ).scalars().all()

        return {
            "total_accounts": {
                "count": total_stats.total_count,
                "total_balance": float(total_stats.total_balance) if total_stats.total_balance else 0
            },
            "by_type": [
                {
                    "type": stat.type.value if stat.type else None,
                    "count": stat.count,
                    "total_balance": float(stat.total_balance) if stat.total_balance else 0
                }
                for stat in type_stats
            ],
            "by_owner_type": [
                {
                    "owner_type": stat.account_owner_type.value,
                    "count": stat.count,
                    "total_balance": float(stat.total_balance) if stat.total_balance else 0
                }
                for stat in owner_type_stats
            ],
            "by_status": [
                {
                    "status": stat.status.value if stat.status else None,
                    "count": stat.count
                }
                for stat in status_stats
            ],
            "top_accounts": [
                {
                    "account_number": acc.account_number,
                    "name": acc.name,
                    "balance": float(acc.balance) if acc.balance else 0,
                    "type": acc.type.value if acc.type else None,
                    "status": acc.status.value if acc.status else None,
                    "account_owner_type": acc.account_owner_type.value
                }
                for acc in top_accounts
            ]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
