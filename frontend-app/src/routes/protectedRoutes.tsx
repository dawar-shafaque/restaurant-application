import { useAuth } from "@/hooks/useAuth";
import  { FC, ReactElement } from "react";
import { Navigate } from "react-router-dom";

type protectedRouteProps = {
    element: ReactElement,
    roles?: string[];
}

export const ProtectedRoute: FC<protectedRouteProps> = ({element, roles}) => {
    const {isAuthenticated, role} = useAuth();
    if(!isAuthenticated) {
        return <Navigate to='/login' replace={true}/>
    }
    if(roles && role && !roles.includes(role)) {
        return <Navigate to='/' />
    }

    return element;
}