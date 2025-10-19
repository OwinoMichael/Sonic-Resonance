import React from 'react';
import { Music2, Search, User, Mic, Library, TrendingUp } from 'lucide-react';

interface AppLayoutProps {
  children: React.ReactNode;
  currentRoute: string;
  navigate: (path: string) => void;
}

export default function AppLayout({ children, currentRoute, navigate }: AppLayoutProps) {
  return (
    <div className="app-container">
      <div className="app-background">
        <div className="gradient-shape gradient-shape-1"></div>
        <div className="gradient-shape gradient-shape-2"></div>
        <div className="gradient-shape gradient-shape-3"></div>
      </div>

      <div className="main-container">
        <header className="app-header">
          <div className="header-glass">
            <div className="header-left">
              <div className="logo-icon">
                <Music2 className="w-5 h-5 text-yellow-400" />
              </div>
              <h1 className="logo-text">Sonic Resonance</h1>
            </div>
            <div className="header-right">
              <button className="header-btn">
                <Search className="w-5 h-5 text-gray-300" />
              </button>
              <button className="header-btn header-btn-active">
                <User className="w-5 h-5 text-yellow-400" />
              </button>
            </div>
          </div>
        </header>

        {children}

        <nav className="bottom-nav">
          <div className="nav-glass">
            <button 
              className={`nav-btn ${currentRoute === '/' ? 'nav-btn-active' : ''}`} 
              onClick={() => navigate('/')}
            >
              <div className={`nav-btn-bg ${currentRoute === '/' ? 'nav-btn-bg-active' : ''}`}>
                <Mic className={`w-6 h-6 ${currentRoute === '/' ? 'text-yellow-400' : 'text-gray-300'}`} />
              </div>
              <span className={`nav-label ${currentRoute === '/' ? 'nav-label-active' : ''}`}>Listen</span>
            </button>
            
            <button 
              className={`nav-btn ${currentRoute === '/library' ? 'nav-btn-active' : ''}`} 
              onClick={() => navigate('/library')}
            >
              <div className={`nav-btn-bg ${currentRoute === '/library' ? 'nav-btn-bg-active' : ''}`}>
                <Library className={`w-6 h-6 ${currentRoute === '/library' ? 'text-yellow-400' : 'text-gray-300'}`} />
              </div>
              <span className={`nav-label ${currentRoute === '/library' ? 'nav-label-active' : ''}`}>Library</span>
            </button>
            
            <button className="nav-btn">
              <div className="nav-btn-bg">
                <TrendingUp className="w-6 h-6 text-gray-300" />
              </div>
              <span className="nav-label">Charts</span>
            </button>
          </div>
        </nav>
      </div>
    </div>
  );
}