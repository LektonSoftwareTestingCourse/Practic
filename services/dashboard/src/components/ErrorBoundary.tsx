import { Component, ErrorInfo, ReactNode } from 'react';
import { ErrorFallback } from './ErrorFallback';

type ErrorBoundaryProps = {
    children: ReactNode;
    name: string;
    fallback?: ReactNode;
    onError?: (error: Error, info: ErrorInfo, name: string) => void;
};

type ErrorBoundaryState = {
    hasError: boolean;
    error: Error | null;
};

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error: Error): ErrorBoundaryState {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
        const { name, onError } = this.props;

        console.error(
            `[ErrorBoundary] Ошибка в виджете "${name}":`,
            error,
            errorInfo.componentStack
        );

        if (onError) {
            onError(error, errorInfo, name);
        }
    }

    resetError = (): void => {
        this.setState({ hasError: false, error: null });
    };

    render() {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback;
            }

            return (
                <ErrorFallback
                    name={this.props.name}
                    error={this.state.error}
                    onRetry={this.resetError}
                />
            );
        }

        return this.props.children;
    }
}
