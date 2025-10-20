import React, { useState, useEffect } from 'react';
import HomePage from './pages/HomePage';
import ListeningPage from './pages/ListeningPage';
import MatchesPage from './pages/MatchesPage';
import NoMatchPage from './pages/NoMatchPage';
import LibraryPage from './pages/LibraryPage';

const BASE_PATH = '/sonicres';

function App() {
  const [currentRoute, setCurrentRoute] = useState('/');

  const navigate = (path: string) => {
    const fullPath = BASE_PATH + path;
    setCurrentRoute(path);
    window.history.pushState({}, '', fullPath);
  };

  useEffect(() => {
    // Get initial route from pathname, removing base path
    const pathname = window.location.pathname;
    const route = pathname.startsWith(BASE_PATH) 
      ? pathname.slice(BASE_PATH.length) || '/'
      : '/';
    setCurrentRoute(route);

    const handlePopState = () => {
      const pathname = window.location.pathname;
      const route = pathname.startsWith(BASE_PATH) 
        ? pathname.slice(BASE_PATH.length) || '/'
        : '/';
      setCurrentRoute(route);
    };
    
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  // Router logic
  const renderPage = () => {
    switch (currentRoute) {
      case '/':
        return <HomePage navigate={navigate} />;
      case '/listening':
        return <ListeningPage navigate={navigate} />;
      case '/matches':
        return <MatchesPage navigate={navigate} />;
      case '/no-match':
        return <NoMatchPage navigate={navigate} />;
      case '/library':
        return <LibraryPage navigate={navigate} />;
      default:
        return <HomePage navigate={navigate} />;
    }
  };

  return <>{renderPage()}</>;
}

export default App;