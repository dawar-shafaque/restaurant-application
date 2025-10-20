import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import ErrorBoundry from "@/pages/handlers/ErrorBoundry";

describe("ErrorBoundry Component", () => {
    const ProblematicComponent = () => {
        throw new Error("Test error");
    };

    it("renders children when no error occurs", () => {
        render(
            <ErrorBoundry>
                <div>Child Component</div>
            </ErrorBoundry>
        );
        expect(screen.getByText("Child Component")).toBeInTheDocument();
    });

    it("renders error message when an error occurs", () => {
        render(
            <ErrorBoundry>
                <ProblematicComponent />
            </ErrorBoundry>
        );
        expect(screen.getByText("Error occurred")).toBeInTheDocument();
        expect(screen.getByText("Test error")).toBeInTheDocument();
    });

    it("logs error and error info to the console", () => {
        const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

        render(
            <ErrorBoundry>
                <ProblematicComponent />
            </ErrorBoundry>
        );

        expect(consoleErrorSpy).toHaveBeenCalledTimes(5);
        expect(consoleErrorSpy).toHaveBeenCalledWith(expect.any(Error));
        expect(consoleErrorSpy).toHaveBeenCalledWith(expect.any(Object));

        consoleErrorSpy.mockRestore();
    });
});