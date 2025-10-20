import { Component, ErrorInfo, ReactNode } from "react";

interface ErrorBoundryProps {
    children: ReactNode;
}
interface ErrorBoundryState {
    hasError:boolean;
    error: Error | null;
}

class ErrorBoundry extends Component<ErrorBoundryProps, ErrorBoundryState> {
    constructor(props: ErrorBoundryProps) {
        super(props);
        this.state = {
            hasError: false,
            error: null,
        }
    }
    static getDerivedStateFromError(error: Error): ErrorBoundryState {
        return {
            hasError: true,
            error: error,
        }
    }
    componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
        console.error(error)
        console.error(errorInfo)
    }
    render(): ReactNode {
        if(this.state.hasError) {
            return (
                <>
                <h1>Error occurred</h1>
                <p>{this.state.error?.message}</p>
                </>
            );
        }
        return this.props.children;
    }
}
export default ErrorBoundry;