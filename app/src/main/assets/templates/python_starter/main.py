"""
CodeForge Studio - Python Starter Project
Run this script locally or in the integrated terminal!
"""

import sys
import platform

def calculate_fibonacci(n: int) -> list:
    fib = [0, 1]
    while len(fib) < n:
        fib.append(fib[-1] + fib[-2])
    return fib[:n]

def main():
    print("=" * 45)
    print(" Welcome to CodeForge Studio (Python Environment)")
    print("=" * 45)
    print(f"Python Version: {platform.python_version()}")
    print(f"Platform: {sys.platform}")
    print()
    
    terms = 10
    print(f"First {terms} Fibonacci numbers:")
    fib_series = calculate_fibonacci(terms)
    print(" ->", fib_series)
    
    print("\nStatus: Real Python execution verified!")

if __name__ == "__main__":
    main()
