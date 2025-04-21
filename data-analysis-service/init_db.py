import sys
import os
from app.analysis.database import Base, engine
# Add the current directory to the Python path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))


def init_db():
    # Create all tables
    Base.metadata.create_all(bind=engine)
    print("Database tables created successfully!")


if __name__ == "__main__":
    init_db() 