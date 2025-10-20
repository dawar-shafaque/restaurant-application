import { Route, Routes } from "react-router-dom";
import { routes } from "./routes/routes";
import { Suspense, FC, lazy } from "react";
import { ToastContainer } from 'react-toastify';

const ErrorBoundry = lazy(() => import("@/pages/handlers/ErrorBoundry"));

const App: FC = () => {
  return (
    <>
      <Suspense fallback={<div className="flex items-center justify-center min-h-screen w-full">Loading...</div>}>
        <ErrorBoundry>
          <Routes>
            {routes.map((obj) => {
              return (
                <Route
                  key={obj.path}
                  path={obj.path}
                  element={<obj.element />}
                />
              );
            })}
          </Routes>
          <ToastContainer 
          hideProgressBar
          autoClose={2500}
          />
        </ErrorBoundry>
      </Suspense>
    </>
  );
};

export default App;
