import { useState, useEffect } from 'react';

import ListeningPage from './pages/ListeningPage';
import MatchesPage from './pages/MatchesPage';
import NoMatchPage from './pages/NoMatchPage';
import LibraryPage from './pages/LibraryPage';
import HomePage from './pages/homepage';
import SongDetailPage from './pages/SongdetailPage';


const BASE_PATH = '/sonicres';

function App() {
  const [currentRoute, setCurrentRoute] = useState('/');

  const navigate = (path: string) => {
    const fullPath = BASE_PATH + path;
    setCurrentRoute(path);
    window.history.pushState({}, '', fullPath);
  };

  useEffect(() => {
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
      case '/song':
        return <SongDetailPage navigate={navigate} />;
      default:
        return <HomePage navigate={navigate} />;
    }
  };

  return <>{renderPage()}</>;
}

export default App;